package com.springlock.lock.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.OptionalLong;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;


@Repository
@Profile({"postgres", "h2"})
@ConditionalOnExpression("'${springlock.lock.strategy:jpa}'.equals('jdbc') and !'optimistic'.equals('${springlock.lock.jdbc.mode:pessimistic}')")
public class JdbcPessimisticLockRepository implements SqlRowLockRepository {

    private static final Instant RELEASED_AT = Instant.EPOCH;

    private static final String SQL_SELECT_FOR_UPDATE =
        "SELECT expires_at FROM distributed_locks WHERE lock_key = ? FOR UPDATE";
    private static final String SQL_UPDATE_TAKE =
        "UPDATE distributed_locks SET owner = ?, expires_at = ?, lock_version = lock_version + 1"
        + " WHERE lock_key = ?";
    private static final String SQL_RELEASE =
        "UPDATE distributed_locks SET expires_at = ?, lock_version = lock_version + 1"
        + " WHERE lock_key = ? AND owner = ? AND expires_at >= ?";
    private static final String SQL_IS_LOCKED =
        "SELECT COUNT(1) FROM distributed_locks WHERE lock_key = ? AND expires_at >= ?";
    private static final String SQL_RENEW =
        "UPDATE distributed_locks SET expires_at = ?, lock_version = lock_version + 1"
        + " WHERE lock_key = ? AND owner = ?";
    private static final String SQL_INSERT =
        "INSERT INTO distributed_locks (lock_key, owner, expires_at, created_at, lock_version)"
        + " VALUES (?, ?, ?, ?, 1)";
    private static final String SQL_SELECT_VERSION =
        "SELECT lock_version FROM distributed_locks WHERE lock_key = ?";

    private final JdbcTemplate jdbcTemplate;

    public JdbcPessimisticLockRepository(JdbcTemplate jdbcTemplate) {
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
        return jdbcTemplate.update(SQL_RELEASE, ts(RELEASED_AT), lockKey, owner, ts(now)) > 0;
    }

    @Override
    public boolean isLocked(String lockKey, Instant now) {
        Integer count = jdbcTemplate.queryForObject(SQL_IS_LOCKED, Integer.class, lockKey, ts(now));
        return count != null && count > 0;
    }

    @Override
    public boolean renew(String lockKey, String owner, Instant expiresAt) {
        return jdbcTemplate.update(SQL_RENEW, ts(expiresAt), lockKey, owner) > 0;
    }

    @Override
    public OptionalLong findVersion(String lockKey) {
        Long v = jdbcTemplate.query(SQL_SELECT_VERSION,
            rs -> rs.next() ? rs.getLong(1) : null, lockKey);
        return v == null ? OptionalLong.empty() : OptionalLong.of(v);
    }

    private boolean insertLockRow(String lockKey, String owner, Instant now, Instant expiresAt) {
        try {
            return jdbcTemplate.update(SQL_INSERT, lockKey, owner, ts(expiresAt), ts(now)) > 0;
        } catch (DataIntegrityViolationException ignored) {
            return false;
        }
    }

    private boolean acquireExistingRow(String lockKey, String owner, Instant now, Instant expiresAt) {
        Timestamp currentExpiresAt = jdbcTemplate.query(SQL_SELECT_FOR_UPDATE,
            rs -> rs.next() ? rs.getTimestamp(1) : null, lockKey);
        if (currentExpiresAt == null) {
            return insertLockRow(lockKey, owner, now, expiresAt);
        }
        if (!currentExpiresAt.toInstant().isBefore(now)) {
            return false;
        }
        return jdbcTemplate.update(SQL_UPDATE_TAKE, owner, ts(expiresAt), lockKey) > 0;
    }

    private Timestamp ts(Instant v) { return Timestamp.from(v); }
}
