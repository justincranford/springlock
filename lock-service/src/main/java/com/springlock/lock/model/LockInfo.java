package com.springlock.lock.model;

import java.time.Instant;

/** Immutable snapshot of a distributed lock''s state. */
public record LockInfo(String lockKey, String owner, Instant expiresAt) {}