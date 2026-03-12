package com.springlock.lock.mapper;

import com.springlock.lock.domain.DistributedLock;
import com.springlock.lock.model.LockInfo;

/** Converts between the {@link DistributedLock} entity and the {@link LockInfo} model. */
public final class LockMapper {

    private LockMapper() {}

    public static LockInfo toLockInfo(DistributedLock lock) {
        return new LockInfo(lock.getLockKey(), lock.getOwner(), lock.getExpiresAt());
    }
}