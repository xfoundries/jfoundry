package org.jfoundry.infrastructure.persistence.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.jfoundry.infrastructure.persistence.jpa.support.GraphOrder;
import org.jfoundry.infrastructure.persistence.jpa.support.GraphOrderEntity;
import org.jfoundry.infrastructure.persistence.jpa.support.GraphOrderId;
import org.jfoundry.infrastructure.persistence.jpa.support.GraphOrderLineEntity;
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
    void modifySynchronizesManagedChildGraphAndOrphanRemoval() {
        GraphOrder order = GraphOrder.create(new GraphOrderId("GRAPH-1"), List.of("A", "B"));
        repository.add(order);
        GraphOrder loaded = repository.findById(order.getId());
        loaded.replaceLines(List.of("B", "C"));
        repository.modify(loaded);

        assertThat(lineSkus("GRAPH-1")).containsExactlyInAnyOrder("B", "C");
        assertThat(lineCount("GRAPH-1")).isEqualTo(2);
    }

    private JpaAggregateRepository<GraphOrder, GraphOrderId, GraphOrderEntity, String> repository(
            EntityManager entityManager) {
        throw new UnsupportedOperationException("Task 2 must provide a mapper-backed repository fixture.");
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
}
