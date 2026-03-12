# Performance Benchmark Report

**Date**: 2026-03-12
**Platform**: Windows, OpenJDK 21.0.3+9 (Temurin)
**Database**: H2 in-memory (PostgreSQL compatibility mode)
**Build**: Spring Boot 4.0.3, Gradle 9.4.0
**Warm-up**: 3 rounds per benchmark (discarded)
**Measured**: 10 rounds per benchmark
**Sequential**: 200 acquire+release ops per round
**Concurrent**: 20 threads x 50 iterations = 1,000 ops per round

## Summary

| Backend | Strategy | Test Type | Ops/Round | Mean (ops/s) | StdDev | Min | Max | CV% |
|---------|----------|-----------|-----------|-------------|--------|-----|-----|-----|
| h2-jdbc-optimistic | JDBC Optimistic | sequential | 200 | 2,734 | 113 | 2,532 | 2,916 | 4.1 |
| h2-jdbc-optimistic | JDBC Optimistic | concurrent-20t | 1,000 | 23,812 | 7,928 | 13,010 | 35,679 | 33.3 |
| h2-jdbc-pessimistic | JDBC Pessimistic | sequential | 200 | 3,634 | 170 | 3,342 | 3,998 | 4.7 |
| h2-jdbc-pessimistic | JDBC Pessimistic | concurrent-20t | 1,000 | 28,409 | 6,887 | 14,101 | 38,562 | 24.2 |
| h2-jpa | JPA | sequential | 200 | 3,511 | 373 | 2,761 | 4,126 | 10.6 |
| h2-jpa | JPA | concurrent-20t | 1,000 | 12,841 | 2,800 | 8,612 | 17,779 | 21.8 |

## Sequential Throughput (single-threaded)

```
h2-jdbc-pessimistic:  3,634 +/- 170 ops/sec  ████████████████████ (best)
h2-jpa:               3,511 +/- 373 ops/sec  ███████████████████
h2-jdbc-optimistic:   2,734 +/- 113 ops/sec  ███████████████
```

- JDBC Pessimistic leads slightly: SELECT FOR UPDATE has minimal overhead per-op
- JPA is competitive sequentially; Hibernate entity lifecycle adds ~3% overhead
- JDBC Optimistic is slowest: CAS pattern (read-compare-write) requires extra SELECT

## Concurrent Throughput (20 threads, same lock key)

```
h2-jdbc-pessimistic: 28,409 +/- 6,887 ops/sec  ████████████████████ (best)
h2-jdbc-optimistic:  23,812 +/- 7,928 ops/sec  █████████████████
h2-jpa:              12,841 +/- 2,800 ops/sec  █████████
```

- JDBC Pessimistic dominates: row-level locks serialize contention efficiently
- JDBC Optimistic is ~84% of pessimistic: CAS retries under contention are costly
- JPA is ~45% of pessimistic: entity manager flush overhead limits concurrency

## Key Findings

1. **JDBC strategies outperform JPA by 1.9-2.2x** under concurrent load
2. **Pessimistic locking is optimal** for high-contention single-key scenarios
3. **Optimistic locking** is viable for moderate contention (avoids full row locks)
4. **Sequential variance** is low (CV 4-11%), confirming measurement stability
5. **Concurrent variance** is higher (CV 22-33%), expected from thread scheduling
6. H2 in-memory represents an upper bound; PostgreSQL adds network + WAL overhead

## Statistical Validity

| Metric | Value |
|--------|-------|
| Measured rounds per benchmark | 10 |
| Warm-up rounds (discarded) | 3 |
| Total measured operations | 36,000 |
| Timer resolution | System.nanoTime() (ns) |
| Coefficient of variation (sequential) | 4.1 - 10.6% |
| Coefficient of variation (concurrent) | 21.8 - 33.3% |

## Backends Requiring Docker

The following backends were not benchmarked (Docker Desktop required):

| Backend | Strategy | Test Class |
|---------|----------|------------|
| PostgreSQL 14-18 | JPA | LockServicePostgres{14..18}JpaIT |
| PostgreSQL 14-18 | JDBC Pessimistic | LockServicePostgres{14..18}JdbcPessimisticIT |
| PostgreSQL 14-18 | JDBC Optimistic | LockServicePostgres{14..18}JdbcOptimisticIT |
| Citus (pg17) | JPA | LockServiceCitusJpaIT |
| Citus (pg17) | JDBC Pessimistic | LockServiceCitusJdbcPessimisticIT |
| Citus (pg17) | JDBC Optimistic | LockServiceCitusJdbcOptimisticIT |
| Redis | Redis StringCommands | LockServiceRedisIT |

To benchmark all backends:
```bash
# Start Docker Desktop, then:
./gradlew integrationTest --rerun
```

## Reproducing Results

```bash
# Run H2 performance benchmarks only:
./gradlew :lock-service:perfTest --rerun

# CSV data is written to:
# lock-service/build/reports/perf/perf-*.csv
```