package org.jfoundry.infrastructure.persistence;

import org.jfoundry.domain.entity.agg.BaseAggregateRoot;
import org.jmolecules.ddd.types.AggregateRoot;
import org.jmolecules.ddd.types.Identifier;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.lang.reflect.TypeVariable;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataConverterTest {

    private final DataConverter<TestEntity, TestId, TestData, String> converter = new DataConverter<>() {
        @Override
        public TestData toData(TestEntity entity) {
            TestData data = new TestData();
            data.setId(toDataId(entity.getId()));
            data.name = entity.name;
            return data;
        }

        @Override
        public TestEntity toEntity(TestData data) {
            return new TestEntity(new TestId(data.getId()), data.name);
        }

        @Override
        public String toDataId(TestId id) {
            return id == null ? null : id.value();
        }
    };

    @Test
    void shouldConvertEntityCollectionToDataListByDefault() {
        List<TestData> dataList = converter.toDataList(List.of(
                new TestEntity(new TestId("1"), "created"),
                new TestEntity(new TestId("2"), "updated")
        ));

        assertEquals(List.of("1", "2"), dataList.stream().map(TestData::getId).toList());
        assertEquals(List.of("created", "updated"), dataList.stream().map(data -> data.name).toList());
    }

    @Test
    void shouldConvertDataCollectionToEntityListByDefault() {
        TestData first = new TestData();
        first.setId("1");
        first.name = "created";
        TestData second = new TestData();
        second.setId("2");
        second.name = "updated";

        List<TestEntity> entities = converter.toEntityList(List.of(first, second));

        assertEquals(List.of("1", "2"), entities.stream().map(e -> e.getId().value()).toList());
        assertEquals(List.of("created", "updated"), entities.stream().map(e -> e.name).toList());
    }

    @Test
    void shouldTreatNullAndEmptyCollectionsAsEmptyLists() {
        assertTrue(converter.toDataList(null).isEmpty());
        assertTrue(converter.toDataList(List.of()).isEmpty());
        assertTrue(converter.toEntityList(null).isEmpty());
        assertTrue(converter.toEntityList(List.of()).isEmpty());
    }

    @Test
    void publicPersistenceAbstractionsShouldUseJavaStyleTypeParameterNames() {
        assertThat(typeParameterNames(DataConverter.class)).containsExactly("T", "ID", "D", "K");
        assertThat(typeParameterNames(AbstractPersistenceRepository.class)).containsExactly("T", "ID", "D", "K");
    }

    private static List<String> typeParameterNames(Class<?> type) {
        return List.of(type.getTypeParameters()).stream().map(TypeVariable::getName).toList();
    }

    record TestId(String value) implements Identifier, Serializable {
    }

    private static final class TestData extends AggregateData<String> {
        private String name;
    }

    private static final class TestEntity extends BaseAggregateRoot<TestEntity, TestId>
            implements AggregateRoot<TestEntity, TestId> {
        private final String name;

        private TestEntity(TestId id, String name) {
            super(id);
            this.name = name;
        }
    }
}
