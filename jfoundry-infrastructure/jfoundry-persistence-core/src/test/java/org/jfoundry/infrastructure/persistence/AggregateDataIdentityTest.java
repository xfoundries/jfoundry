package org.jfoundry.infrastructure.persistence;

import org.junit.jupiter.api.Test;

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
        AggregateData<String> a = new TestData();
        AggregateData<String> b = new TestData();

        assertNotEquals(a, b);
        assertFalse(a.equals(b));
    }

    @Test
    void newEntitiesWithNullIdShouldNotCollapseInHashSet() {
        AggregateData<String> a = new TestData();
        AggregateData<String> b = new TestData();

        Set<AggregateData<String>> set = new HashSet<>();
        set.add(a);
        set.add(b);

        assertEquals(2, set.size(), "两个未持久化对象应能共存于 HashSet");
    }

    @Test
    void entitiesWithSameIdShouldBeEqual() {
        AggregateData<String> a = new TestData();
        a.setId("1");
        AggregateData<String> b = new TestData();
        b.setId("1");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void entitiesWithDifferentIdShouldNotBeEqual() {
        AggregateData<String> a = new TestData();
        a.setId("1");
        AggregateData<String> b = new TestData();
        b.setId("2");

        assertNotEquals(a, b);
    }

    @Test
    void nullIdEntityShouldNotEqualPersistedEntity() {
        AggregateData<String> a = new TestData();
        AggregateData<String> b = new TestData();
        b.setId("1");

        assertNotEquals(a, b);
        assertNotEquals(b, a);
    }

    @Test
    void sameReferenceShouldBeEqual() {
        AggregateData<String> a = new TestData();

        assertTrue(a.equals(a));
    }

    @Test
    void shouldNotEqualNullOrDifferentType() {
        AggregateData<String> a = new TestData();
        a.setId("1");

        assertFalse(a.equals(null));
        assertFalse(a.equals("not an AggregateData"));
    }

    private static final class TestData extends AggregateData<String> {
    }
}
