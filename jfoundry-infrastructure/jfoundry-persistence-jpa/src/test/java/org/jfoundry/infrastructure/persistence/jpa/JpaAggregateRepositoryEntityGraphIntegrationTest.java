package org.jfoundry.infrastructure.persistence.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.jfoundry.infrastructure.persistence.jpa.support.GraphOrder;
import org.jfoundry.infrastructure.persistence.jpa.support.GraphOrderEntity;
import org.jfoundry.infrastructure.persistence.jpa.support.GraphOrderId;
import org.jfoundry.infrastructure.persistence.jpa.support.GraphOrderLineEntity;
import org.jfoundry.infrastructure.persistence.jpa.support.GraphOrderMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JpaAggregateRepositoryEntityGraphIntegrationTest {

    private static EntityManagerFactory entityManagerFactory;

    private EntityManager entityManager;
    private JpaAggregateRepository<GraphOrder, GraphOrderId, GraphOrderEntity, String> repository;

    @BeforeAll
    static void createEntityManagerFactory() {
        entityManagerFactory = Persistence.createEntityManagerFactory("jfoundry-jpa-test");
    }

    @AfterAll
    static void closeEntityManagerFactory() {
        entityManagerFactory.close();
    }

    @BeforeEach
    void beginTransaction() {
        entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();
        repository = repository(entityManager);
    }

    @AfterEach
    void closeEntityManager() {
        if (entityManager != null) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
        }
    }

    @Test
    void addPersistsRootAndChildLines() {
        GraphOrder order = GraphOrder.create(new GraphOrderId("GRAPH-1"), List.of("A", "B"));

        repository.add(order);

        assertThat(entityManager.find(GraphOrderEntity.class, "GRAPH-1")).isNotNull();
        assertThat(lineSkus("GRAPH-1")).containsExactlyInAnyOrder("A", "B");
        assertThat(lineCount("GRAPH-1")).isEqualTo(2);
    }

    @Test
    void modifyReplacesManagedChildGraphAndRemovesOrphans() {
        GraphOrder order = GraphOrder.create(new GraphOrderId("GRAPH-1"), List.of("A", "B"));
        repository.add(order);
        GraphOrder loaded = repository.findById(order.getId());
        loaded.replaceLines(List.of("B", "C"));
        repository.modify(loaded);

        assertThat(lineSkus("GRAPH-1")).containsExactlyInAnyOrder("B", "C");
        assertThat(lineCount("GRAPH-1")).isEqualTo(2);
    }

    @Test
    void modifyAdvancesRootVersion() {
        GraphOrder order = GraphOrder.create(new GraphOrderId("GRAPH-1"), List.of("A", "B"));
        repository.add(order);
        GraphOrder loaded = repository.findById(order.getId());
        GraphOrderEntity managed = entityManager.find(GraphOrderEntity.class, "GRAPH-1");
        long versionBeforeModify = managed.version();

        loaded.replaceLines(List.of("B", "C"));
        repository.modify(loaded);

        assertThat(managed.version()).isGreaterThan(versionBeforeModify);
    }

    private JpaAggregateRepository<GraphOrder, GraphOrderId, GraphOrderEntity, String> repository(
            EntityManager entityManager) {
        GraphRepository repository = new GraphRepository(entityManager, new GraphOrderMapper());
        repository.setAggregatePersistenceContext(new TestPersistenceContext());
        return repository;
    }

    private List<String> lineSkus(String orderId) {
        return entityManager.createQuery(
                        "select line.sku from GraphOrderLineEntity line where line.order.id = :orderId",
                        String.class)
                .setParameter("orderId", orderId)
                .getResultList();
    }

    private long lineCount(String orderId) {
        return entityManager.createQuery(
                        "select count(line) from GraphOrderLineEntity line where line.order.id = :orderId",
                        Long.class)
                .setParameter("orderId", orderId)
                .getSingleResult();
    }

    private static final class GraphRepository extends
            JpaAggregateRepository<GraphOrder, GraphOrderId, GraphOrderEntity, String> {

        private GraphRepository(EntityManager entityManager, GraphOrderMapper mapper) {
            super(entityManager, GraphOrderEntity.class, mapper);
        }
    }
}
