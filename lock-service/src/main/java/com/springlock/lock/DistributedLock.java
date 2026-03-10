package com.springlock.lock;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
    name = "distributed_locks",
    uniqueConstraints = @UniqueConstraint(name = "uq_lock_key", columnNames = "lock_key")
)
public class DistributedLock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lock_key", nullable = false, length = 255)
    private String lockKey;

    @Column(nullable = false, length = 255)
    private String owner;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DistributedLock() {}

    public DistributedLock(String lockKey, String owner, Instant expiresAt) {
        this.lockKey = lockKey;
        this.owner = owner;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getLockKey() { return lockKey; }
    public String getOwner() { return owner; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }

    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
