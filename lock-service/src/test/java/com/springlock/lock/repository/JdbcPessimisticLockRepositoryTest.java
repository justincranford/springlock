package com.springlock.lock.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import com.springlock.lock.model.LockInfo;

@ExtendWith(MockitoExtension.class)
class JdbcPessimisticLockRepositoryTest {

    @Mock
    JdbcTemplate jdbcTemplate;

    JdbcPessimisticLockRepository repo;

    @BeforeEach
    void setUp() {
        repo = new JdbcPessimisticLockRepository(jdbcTemplate);
    }

    @Test
    void tryAcquire_whenInsertSucceeds_returnsTrue() {
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any())).thenReturn(1);

        assertThat(repo.tryAcquire("key", "owner", Instant.now(), Instant.now().plusSeconds(300))).isTrue();
    }

    @Test
    void tryAcquire_whenInsertFailsAndRowIsNull_retriesInsertSuccessfully() {
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any()))
            .thenReturn(0)
            .thenReturn(1);
        doAnswer(inv -> {
            ResultSetExtractor<?> rse = inv.getArgument(1);
            ResultSet rs = mock(ResultSet.class);
            when(rs.next()).thenReturn(false);
            return rse.extractData(rs);
        }).when(jdbcTemplate).query(anyString(), any(ResultSetExtractor.class), anyString());

        boolean result = repo.tryAcquire("key", "owner", Instant.now(), Instant.now().plusSeconds(300));

        assertThat(result).isTrue();
    }

    @Test
    void tryAcquire_whenInsertFailsAndRowIsNull_retriesInsertFails() {
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any())).thenReturn(0);
        doAnswer(inv -> {
            ResultSetExtractor<?> rse = inv.getArgument(1);
            ResultSet rs = mock(ResultSet.class);
            when(rs.next()).thenReturn(false);
            return rse.extractData(rs);
        }).when(jdbcTemplate).query(anyString(), any(ResultSetExtractor.class), anyString());

        boolean result = repo.tryAcquire("key", "owner", Instant.now(), Instant.now().plusSeconds(300));

        assertThat(result).isFalse();
    }

    @Test
    void tryAcquire_whenInsertFailsAndRowIsLocked_returnsFalse() {
        Instant future = Instant.now().plusSeconds(300);
        Instant now = Instant.now();
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any())).thenReturn(0);
        doAnswer(inv -> {
            ResultSetExtractor<?> rse = inv.getArgument(1);
            ResultSet rs = mock(ResultSet.class);
            when(rs.next()).thenReturn(true);
            when(rs.getTimestamp(1)).thenReturn(Timestamp.from(future));
            return rse.extractData(rs);
        }).when(jdbcTemplate).query(anyString(), any(ResultSetExtractor.class), anyString());

        boolean result = repo.tryAcquire("key", "owner", now, now.plusSeconds(600));

        assertThat(result).isFalse();
    }

    @Test
    void tryAcquire_whenInsertFailsAndRowExpired_takesLock() {
        Instant now = Instant.now();
        Instant expiredAt = Instant.now().minusSeconds(60);
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any())).thenReturn(0);
        doAnswer(inv -> {
            ResultSetExtractor<?> rse = inv.getArgument(1);
            ResultSet rs = mock(ResultSet.class);
            when(rs.next()).thenReturn(true);
            when(rs.getTimestamp(1)).thenReturn(Timestamp.from(expiredAt));
            return rse.extractData(rs);
        }).when(jdbcTemplate).query(anyString(), any(ResultSetExtractor.class), anyString());
        when(jdbcTemplate.update(anyString(), anyString(), any(), anyString())).thenReturn(1);

        boolean result = repo.tryAcquire("key", "owner", now, now.plusSeconds(300));

        assertThat(result).isTrue();
    }

    @Test
    void tryAcquire_whenInsertFailsAndRowExpired_butUpdateFails_returnsFalse() {
        Instant now = Instant.now();
        Instant expiredAt = Instant.now().minusSeconds(60);
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any())).thenReturn(0);
        doAnswer(inv -> {
            ResultSetExtractor<?> rse = inv.getArgument(1);
            ResultSet rs = mock(ResultSet.class);
            when(rs.next()).thenReturn(true);
            when(rs.getTimestamp(1)).thenReturn(Timestamp.from(expiredAt));
            return rse.extractData(rs);
        }).when(jdbcTemplate).query(anyString(), any(ResultSetExtractor.class), anyString());
        when(jdbcTemplate.update(anyString(), anyString(), any(), anyString())).thenReturn(0);

        boolean result = repo.tryAcquire("key", "owner", now, now.plusSeconds(300));

        assertThat(result).isFalse();
    }

    @Test
    void tryAcquire_whenRowExpiresExactlyAtNow_returnsFalse() {
        Instant now = Instant.now();
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any())).thenReturn(0);
        doAnswer(inv -> {
            ResultSetExtractor<?> rse = inv.getArgument(1);
            ResultSet rs = mock(ResultSet.class);
            when(rs.next()).thenReturn(true);
            when(rs.getTimestamp(1)).thenReturn(Timestamp.from(now));
            return rse.extractData(rs);
        }).when(jdbcTemplate).query(anyString(), any(ResultSetExtractor.class), anyString());

        boolean result = repo.tryAcquire("key", "owner", now, now.plusSeconds(300));

        assertThat(result).isFalse();
    }

    @Test
    void release_whenUpdateSucceeds_returnsTrue() {
        when(jdbcTemplate.update(anyString(), any(), anyString(), anyString(), any())).thenReturn(1);

        assertThat(repo.release("key", "owner", Instant.now())).isTrue();
    }

    @Test
    void release_whenUpdateFails_returnsFalse() {
        when(jdbcTemplate.update(anyString(), any(), anyString(), anyString(), any())).thenReturn(0);

        assertThat(repo.release("key", "owner", Instant.now())).isFalse();
    }

    @Test
    void release_passesTimestampToJdbc() {
        Instant now = Instant.now();
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Timestamp> captor = ArgumentCaptor.forClass(Timestamp.class);
        when(jdbcTemplate.update(anyString(), captor.capture(), anyString(), anyString(), captor.capture())).thenReturn(1);

        repo.release("key", "owner", now);

        assertThat(captor.getAllValues().get(0)).isNotNull();
        assertThat(captor.getAllValues().get(1)).isNotNull();
        assertThat(captor.getAllValues().get(1)).isEqualTo(Timestamp.from(now));
    }

    @Test
    void isLocked_whenCountPositive_returnsTrue() {
        when(jdbcTemplate.queryForObject(anyString(), any(Class.class), any(), any())).thenReturn(1);

        assertThat(repo.isLocked("key", Instant.now())).isTrue();
    }

    @Test
    void isLocked_whenCountZero_returnsFalse() {
        when(jdbcTemplate.queryForObject(anyString(), any(Class.class), any(), any())).thenReturn(0);

        assertThat(repo.isLocked("key", Instant.now())).isFalse();
    }

    @Test
    void isLocked_whenCountNull_returnsFalse() {
        when(jdbcTemplate.queryForObject(anyString(), any(Class.class), any(), any())).thenReturn(null);

        assertThat(repo.isLocked("key", Instant.now())).isFalse();
    }

    @Test
    void renew_whenUpdateSucceeds_returnsTrue() {
        when(jdbcTemplate.update(anyString(), any(), anyString(), anyString())).thenReturn(1);

        assertThat(repo.renew("key", "owner", Instant.now().plusSeconds(300))).isTrue();
    }

    @Test
    void renew_whenUpdateFails_returnsFalse() {
        when(jdbcTemplate.update(anyString(), any(), anyString(), anyString())).thenReturn(0);

        assertThat(repo.renew("key", "owner", Instant.now().plusSeconds(300))).isFalse();
    }

    @Test
    void renew_passesTimestampToJdbc() {
        Instant expiresAt = Instant.now().plusSeconds(300);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Timestamp> captor = ArgumentCaptor.forClass(Timestamp.class);
        when(jdbcTemplate.update(anyString(), captor.capture(), anyString(), anyString())).thenReturn(1);

        repo.renew("key", "owner", expiresAt);

        assertThat(captor.getValue()).isNotNull();
        assertThat(captor.getValue()).isEqualTo(Timestamp.from(expiresAt));
    }

    @Test
    void findLockInfo_whenActive_returnsLockInfo() throws Exception {
        Instant expiresAt = Instant.now().plusSeconds(300);
        doAnswer(inv -> {
            ResultSetExtractor<?> rse = inv.getArgument(1);
            ResultSet rs = mock(ResultSet.class);
            when(rs.next()).thenReturn(true);
            when(rs.getString(1)).thenReturn("owner");
            when(rs.getTimestamp(2)).thenReturn(Timestamp.from(expiresAt));
            return rse.extractData(rs);
        }).when(jdbcTemplate).query(anyString(), any(ResultSetExtractor.class), any(), any());

        Optional<LockInfo> info = repo.findLockInfo("key", Instant.now());

        assertThat(info).isPresent();
        assertThat(info.get().lockKey()).isEqualTo("key");
        assertThat(info.get().owner()).isEqualTo("owner");
    }

    @Test
    void findLockInfo_whenNoRow_returnsEmpty() throws Exception {
        doAnswer(inv -> {
            ResultSetExtractor<?> rse = inv.getArgument(1);
            ResultSet rs = mock(ResultSet.class);
            when(rs.next()).thenReturn(false);
            return rse.extractData(rs);
        }).when(jdbcTemplate).query(anyString(), any(ResultSetExtractor.class), any(), any());

        Optional<LockInfo> info = repo.findLockInfo("key", Instant.now());

        assertThat(info).isEmpty();
    }

    @Test
    void findVersion_whenFound_returnsVersion() throws Exception {
        doAnswer(inv -> {
            ResultSetExtractor<?> rse = inv.getArgument(1);
            ResultSet rs = mock(ResultSet.class);
            when(rs.next()).thenReturn(true);
            when(rs.getLong(1)).thenReturn(5L);
            return rse.extractData(rs);
        }).when(jdbcTemplate).query(anyString(), any(ResultSetExtractor.class), anyString());

        assertThat(repo.findVersion("key")).hasValue(5L);
    }

    @Test
    void findVersion_whenNotFound_returnsEmpty() throws Exception {
        doAnswer(inv -> {
            ResultSetExtractor<?> rse = inv.getArgument(1);
            ResultSet rs = mock(ResultSet.class);
            when(rs.next()).thenReturn(false);
            return rse.extractData(rs);
        }).when(jdbcTemplate).query(anyString(), any(ResultSetExtractor.class), anyString());

        assertThat(repo.findVersion("key")).isEqualTo(OptionalLong.empty());
    }
}
