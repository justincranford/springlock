package com.springlock.lock.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.springlock.lock.model.LockInfo;

@Repository
@Profile({"postgres", "h2"})
@ConditionalOnExpression("'${springlock.lock.strategy:jpa}'.equals('jdbc') and 'optimistic'.equals('${springlock.lock.jdbc.mode:pessimistic}')")
public class JdbcOptimisticLockRepository implements SqlRowLockRepository {

    private static final Instant RELEASED_AT = Instant.EPOCH;

    private static final String SQL_SELECT_LOCK =
        "SELECT owner, expires_at, lock_version FROM distributed_locks WHERE lock_key = ?";
    private static final String SQL_UPDATE_EXPIRED =
        "UPDATE distributed_locks SET owner = ?, expires_at = ?, lock_version = lock_version + 1"
        + " WHERE lock_key = ? AND expires_at < ? AND lock_version = ?";
    private static final String SQL_RELEASE =
        "UPDATE distributed_locks SET expires_at = ?, lock_version = lock_version + 1"
        + " WHERE lock_key = ? AND owner = ? AND expires_at >= ?";
    private static final String SQL_IS_LOCKED =
        "SELECT COUNT(1) FROM distributed_locks WHERE lock_key = ? AND expires_at >= ?";
    private static final String SQL_RENEW =
        "UPDATE distributed_locks SET expires_at = ?, lock_version = lock_version + 1"
        + " WHERE lock_key = ? AND owner = ? AND expires_at >= ?";
    private static final String SQL_INSERT =
        "INSERT INTO distributed_locks (lock_key, owner, expires_at, created_at, lock_version)"
        + " VALUES (?, ?, ?, ?, 1) ON CONFLICT DO NOTHING";
    private static final String SQL_SELECT_VERSION =
        "SELECT lock_version FROM distributed_locks WHERE lock_key = ?";

    private final JdbcTemplate jdbcTemplate;

    public JdbcOptimisticLockRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean tryAcquire(String lockKey, String owner, Instant now, Instant expiresAt) {
        if (insertLockRow(lockKey, owner, now, expiresAt)) {
            return true;
        }
        return acquireExpiredRow(lockKey, owner, now, expiresAt);
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
        Instant now = Instant.now();
        return jdbcTemplate.update(SQL_RENEW, ts(expiresAt), lockKey, owner, ts(now)) > 0;
    }

    @Override
    public Optional<LockInfo> findLockInfo(String lockKey, Instant now) {
        return Optional.ofNullable(jdbcTemplate.query(SQL_SELECT_LOCK, rs -> {
            if (!rs.next()) return null;
            Instant expiresAt = rs.getTimestamp(2).toInstant();
            if (expiresAt.isBefore(now)) return null;
            return new LockInfo(lockKey, rs.getString(1), expiresAt);
        }, lockKey));
    }

    @Override
    public OptionalLong findVersion(String lockKey) {
        Long v = jdbcTemplate.query(SQL_SELECT_VERSION,
            rs -> rs.next() ? rs.getLong(1) : null, lockKey);
        return v == null ? OptionalLong.empty() : OptionalLong.of(v);
    }

    private boolean insertLockRow(String lockKey, String owner, Instant now, Instant expiresAt) {
        return jdbcTemplate.update(SQL_INSERT, lockKey, owner, ts(expiresAt), ts(now)) > 0;
    }

    private static final class LockState {
        final String owner;
        final Instant expiresAt;
        final long version;
        LockState(String owner, Instant expiresAt, long version) {
            this.owner = owner;
            this.expiresAt = expiresAt;
            this.version = version;
        }
    }

    private boolean acquireExpiredRow(String lockKey, String owner, Instant now, Instant expiresAt) {
        LockState state = jdbcTemplate.query(SQL_SELECT_LOCK, rs -> {
            if (!rs.next()) return null;
            return new LockState(rs.getString(1), rs.getTimestamp(2).toInstant(), rs.getLong(3));
        }, lockKey);
        if (state == null) {
            return insertLockRow(lockKey, owner, now, expiresAt);
        }
        if (!state.expiresAt.isBefore(now)) {
            return false;
        }
        return jdbcTemplate.update(SQL_UPDATE_EXPIRED,
            owner, ts(expiresAt), lockKey, ts(now), state.version) > 0;
    }

    private Timestamp ts(Instant v) { return Timestamp.from(v); }
}
