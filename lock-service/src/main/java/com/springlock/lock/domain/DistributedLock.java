package com.springlock.lock.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "distributed_locks", uniqueConstraints = @UniqueConstraint(name = "uq_lock_key", columnNames = "lock_key"))
public class DistributedLock extends AbstractEntity {

    static final long INITIAL_VERSION = 1L;

    @Column(name = "lock_key", nullable = false, length = 255)
    private String lockKey;

    @Column(nullable = false, length = 255)
    private String owner;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "lock_version", nullable = false)
    private long lockVersion;

    protected DistributedLock() {}

    public DistributedLock(String lockKey, String owner, Instant expiresAt) {
        this.lockKey = lockKey;
        this.owner = owner;
        this.expiresAt = expiresAt;
        this.lockVersion = INITIAL_VERSION;
    }

    public String getLockKey() { return lockKey; }
    public String getOwner() { return owner; }
    public Instant getExpiresAt() { return expiresAt; }
    public long getLockVersion() { return lockVersion; }

    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}