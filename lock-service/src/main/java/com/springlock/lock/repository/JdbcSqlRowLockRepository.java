package com.springlock.lock.repository;

import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.OptionalLong;

@Repository
@Profile({"postgres", "h2"})
public class JdbcSqlRowLockRepository implements SqlRowLockRepository {

    private static final Instant RELEASED_AT = Instant.EPOCH;

    private final JdbcTemplate jdbcTemplate;

    public JdbcSqlRowLockRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean tryAcquire(String lockKey, String owner, Instant now, Instant expiresAt) {
        if (insertLockRow(lockKey, owner, now, expiresAt)) {
            return true;
        }
        return acquireExistingRow(lockKey, owner, now, expiresAt);
    }

    @Override
    public boolean release(String lockKey, String owner, Instant now) {
        int updated = jdbcTemplate.update(
            """
            UPDATE distributed_locks
               SET expires_at = ?,
                   lock_version = lock_version + 1
             WHERE lock_key = ?
               AND owner = ?
               AND expires_at >= ?
            """,
            toTimestamp(RELEASED_AT),
            lockKey,
            owner,
            toTimestamp(now)
        );
        return updated > 0;
    }

    @Override
    public boolean isLocked(String lockKey, Instant now) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(1) FROM distributed_locks WHERE lock_key = ? AND expires_at >= ?",
            Integer.class,
            lockKey,
            toTimestamp(now)
        );
        return count != null && count > 0;
    }

    @Override
    public boolean renew(String lockKey, String owner, Instant expiresAt) {
        int updated = jdbcTemplate.update(
            """
            UPDATE distributed_locks
               SET expires_at = ?,
                   lock_version = lock_version + 1
             WHERE lock_key = ?
               AND owner = ?
            """,
            toTimestamp(expiresAt),
            lockKey,
            owner
        );
        return updated > 0;
    }

    @Override
    public OptionalLong findVersion(String lockKey) {
        Long value = jdbcTemplate.query(
            "SELECT lock_version FROM distributed_locks WHERE lock_key = ?",
            rs -> rs.next() ? rs.getLong(1) : null,
            lockKey
        );
        return value == null ? OptionalLong.empty() : OptionalLong.of(value);
    }

    private boolean insertLockRow(String lockKey, String owner, Instant now, Instant expiresAt) {
        try {
            int inserted = jdbcTemplate.update(
                """
                INSERT INTO distributed_locks (lock_key, owner, expires_at, created_at, lock_version)
                VALUES (?, ?, ?, ?, 1)
                """,
                lockKey,
                owner,
                toTimestamp(expiresAt),
                toTimestamp(now)
            );
            return inserted > 0;
        } catch (DataIntegrityViolationException ignored) {
            return false;
        }
    }

    private boolean acquireExistingRow(String lockKey, String owner, Instant now, Instant expiresAt) {
        Timestamp currentExpiresAt = jdbcTemplate.query(
            "SELECT expires_at FROM distributed_locks WHERE lock_key = ? FOR UPDATE",
            rs -> rs.next() ? rs.getTimestamp(1) : null,
            lockKey
        );

        if (currentExpiresAt == null) {
            return insertLockRow(lockKey, owner, now, expiresAt);
        }

        Instant currentExpiry = currentExpiresAt.toInstant();
        if (!currentExpiry.isBefore(now)) {
            return false;
        }

        int updated = jdbcTemplate.update(
            """
            UPDATE distributed_locks
               SET owner = ?,
                   expires_at = ?,
                   lock_version = lock_version + 1
             WHERE lock_key = ?
            """,
            owner,
            toTimestamp(expiresAt),
            lockKey
        );
        return updated > 0;
    }

    private Timestamp toTimestamp(Instant value) {
        return Timestamp.from(value);
    }
}
