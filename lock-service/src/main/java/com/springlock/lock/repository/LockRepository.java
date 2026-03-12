package com.springlock.lock.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.springlock.lock.domain.DistributedLock;

public interface LockRepository extends JpaRepository<DistributedLock, Long> {

    String JPQL_DELETE_BY_KEY_AND_OWNER =
        "DELETE FROM DistributedLock d WHERE d.lockKey = :lockKey AND d.owner = :owner";
    String JPQL_DELETE_EXPIRED = "DELETE FROM DistributedLock d WHERE d.expiresAt < :now";

    Optional<DistributedLock> findByLockKeyAndOwner(String lockKey, String owner);

    Optional<DistributedLock> findByLockKeyAndExpiresAtGreaterThanEqual(String lockKey, Instant now);

    boolean existsByLockKeyAndExpiresAtGreaterThanEqual(String lockKey, Instant now);

    @Modifying
    @Transactional
    @Query(JPQL_DELETE_BY_KEY_AND_OWNER)
    int deleteByLockKeyAndOwner(@Param("lockKey") String lockKey, @Param("owner") String owner);

    @Modifying
    @Transactional
    @Query(JPQL_DELETE_EXPIRED)
    int deleteExpired(@Param("now") Instant now);
}
