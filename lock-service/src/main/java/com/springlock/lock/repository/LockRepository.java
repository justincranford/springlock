package com.springlock.lock.repository;

import com.springlock.lock.domain.DistributedLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.Optional;

public interface LockRepository extends JpaRepository<DistributedLock, Long> {

    Optional<DistributedLock> findByLockKey(String lockKey);

    Optional<DistributedLock> findByLockKeyAndOwner(String lockKey, String owner);

    boolean existsByLockKeyAndExpiresAtGreaterThanEqual(String lockKey, Instant now);

    @Modifying
    @Query("DELETE FROM DistributedLock d WHERE d.lockKey = :lockKey AND d.owner = :owner")
    int deleteByLockKeyAndOwner(@Param("lockKey") String lockKey, @Param("owner") String owner);

    @Modifying
    @Query("DELETE FROM DistributedLock d WHERE d.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}