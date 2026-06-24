package org.jfoundry.infrastructure.persistence;

import org.jmolecules.ddd.types.Identifier;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// 验证 {@link AggregateData} 的 equals/hashCode 契约。
/// <p>
/// 重点回归：未持久化对象（id == null）不应被 HashSet/HashMap 折叠为同一个元素。
class AggregateDataIdentityTest {

    @Test
    void aggregateDataShouldBeThePersistenceBaseType() {
        assertEquals(Object.class, AggregateData.class.getSuperclass());
    }

    @Test
    void newEntitiesWithNullIdShouldNotBeEqual() {
        AggregateData<TestId> a = new TestData();
        AggregateData<TestId> b = new TestData();

        assertNotEquals(a, b);
        assertFalse(a.equals(b));
    }

    @Test
    void newEntitiesWithNullIdShouldNotCollapseInHashSet() {
        AggregateData<TestId> a = new TestData();
        AggregateData<TestId> b = new TestData();

        Set<AggregateData<TestId>> set = new HashSet<>();
        set.add(a);
        set.add(b);

        assertEquals(2, set.size(), "两个未持久化对象应能共存于 HashSet");
    }

    @Test
    void entitiesWithSameIdShouldBeEqual() {
        AggregateData<TestId> a = new TestData();
        a.setId(new TestId("1"));
        AggregateData<TestId> b = new TestData();
        b.setId(new TestId("1"));

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void entitiesWithDifferentIdShouldNotBeEqual() {
        AggregateData<TestId> a = new TestData();
        a.setId(new TestId("1"));
        AggregateData<TestId> b = new TestData();
        b.setId(new TestId("2"));

        assertNotEquals(a, b);
    }

    @Test
    void nullIdEntityShouldNotEqualPersistedEntity() {
        AggregateData<TestId> a = new TestData();
        AggregateData<TestId> b = new TestData();
        b.setId(new TestId("1"));

        assertNotEquals(a, b);
        assertNotEquals(b, a);
    }

    @Test
    void sameReferenceShouldBeEqual() {
        AggregateData<TestId> a = new TestData();

        assertTrue(a.equals(a));
    }

    @Test
    void shouldNotEqualNullOrDifferentType() {
        AggregateData<TestId> a = new TestData();
        a.setId(new TestId("1"));

        assertFalse(a.equals(null));
        assertFalse(a.equals("not an AggregateData"));
    }

    record TestId(String value) implements Identifier, Serializable {
    }

    private static final class TestData extends AggregateData<TestId> {
    }
}
