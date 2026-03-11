# JDBC Lock Schema Notes

This module currently persists JDBC lock state in `distributed_locks` to keep
`LockService` TTL semantics (`acquire`, `release`, `isLocked`, `renew`) and
version increments (`lock_version = lock_version + 1`).

If you want an `INT_LOCK` table for optimistic locking (JdbcLockRegistry style),
use the following PostgreSQL and H2 variants.

## PostgreSQL / Citus

```sql
CREATE UNLOGGED TABLE IF NOT EXISTS int_lock (
    lock_key UUID NOT NULL,
    region VARCHAR(100) NOT NULL,
    client_id UUID,
    created_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL,
    lock_version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (lock_key, region)
);

CREATE INDEX IF NOT EXISTS idx_int_lock_region_expiry
    ON int_lock (region, expires_at);
```

Notes:
- `UNLOGGED` reduces WAL for non-critical lock state.
- `UUID` avoids string parsing/size overhead for identifiers.
- For Citus, distribute by `lock_key` if lock contention is high and cross-shard
  transactions are not required for this table.

## H2

```sql
CREATE TABLE IF NOT EXISTS int_lock (
    lock_key UUID NOT NULL,
    region VARCHAR(100) NOT NULL,
    client_id UUID,
    created_date TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    lock_version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (lock_key, region)
);

CREATE INDEX IF NOT EXISTS idx_int_lock_region_expiry
    ON int_lock (region, expires_at);
```
