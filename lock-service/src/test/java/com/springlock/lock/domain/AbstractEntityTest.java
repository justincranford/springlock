package com.springlock.lock.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.time.Instant;

import org.junit.jupiter.api.Test;

class AbstractEntityTest {

    static class ConcreteEntity extends AbstractEntity {
        ConcreteEntity() { super(); }
    }

    private static void setId(AbstractEntity entity, Long id) throws Exception {
        Field field = AbstractEntity.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(entity, id);
    }

    @Test
    void newEntity_hasNullIdAndNullCreatedAt() {
        ConcreteEntity entity = new ConcreteEntity();
        assertThat(entity.getId()).isNull();
        assertThat(entity.getCreatedAt()).isNull();
    }

    @Test
    void onPrePersist_setsCreatedAt() {
        ConcreteEntity entity = new ConcreteEntity();
        Instant before = Instant.now();
        entity.onPrePersist();
        Instant after = Instant.now();
        assertThat(entity.getCreatedAt()).isBetween(before, after);
    }

    @Test
    void onPrePersist_doesNotOverwriteExistingCreatedAt() {
        ConcreteEntity entity = new ConcreteEntity();
        entity.onPrePersist();
        Instant firstCreatedAt = entity.getCreatedAt();
        entity.onPrePersist();
        assertThat(entity.getCreatedAt()).isEqualTo(firstCreatedAt);
    }

    @Test
    void equals_sameInstance_returnsTrue() {
        ConcreteEntity entity = new ConcreteEntity();
        assertThat(entity.equals(entity)).isTrue();
    }

    @Test
    void equals_nullOrDifferentClass_returnsFalse() {
        ConcreteEntity entity = new ConcreteEntity();
        assertThat(entity.equals(null)).isFalse();
        assertThat(entity.equals("not an entity")).isFalse();
    }

    @Test
    void equals_twoNewEntitiesWithNullIds_returnsFalse() {
        ConcreteEntity a = new ConcreteEntity();
        ConcreteEntity b = new ConcreteEntity();
        assertThat(a.equals(b)).isFalse();
    }

    @Test
    void equals_sameId_returnsTrue() throws Exception {
        ConcreteEntity a = new ConcreteEntity();
        ConcreteEntity b = new ConcreteEntity();
        setId(a, 42L);
        setId(b, 42L);
        assertThat(a.equals(b)).isTrue();
    }

    @Test
    void equals_differentId_returnsFalse() throws Exception {
        ConcreteEntity a = new ConcreteEntity();
        ConcreteEntity b = new ConcreteEntity();
        setId(a, 1L);
        setId(b, 2L);
        assertThat(a.equals(b)).isFalse();
    }

    @Test
    void equals_oneNullIdOneSet_returnsFalse() throws Exception {
        ConcreteEntity a = new ConcreteEntity();
        ConcreteEntity b = new ConcreteEntity();
        setId(b, 1L);
        assertThat(a.equals(b)).isFalse();
    }

    @Test
    void hashCode_isBasedOnClass() {
        ConcreteEntity a = new ConcreteEntity();
        ConcreteEntity b = new ConcreteEntity();
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a.hashCode()).isEqualTo(ConcreteEntity.class.hashCode());
    }

    @Test
    void toString_containsClassNameAndId() {
        ConcreteEntity entity = new ConcreteEntity();
        assertThat(entity.toString()).contains("ConcreteEntity").contains("id=null");
    }
}
