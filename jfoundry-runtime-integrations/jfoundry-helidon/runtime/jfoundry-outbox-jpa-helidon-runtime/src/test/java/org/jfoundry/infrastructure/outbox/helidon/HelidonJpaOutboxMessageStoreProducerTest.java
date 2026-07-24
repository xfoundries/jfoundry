package org.jfoundry.infrastructure.outbox.helidon;

import jakarta.persistence.EntityManager;
import org.jfoundry.application.outbox.OutboxMessageStore;
import org.jfoundry.infrastructure.outbox.jpa.JpaOutboxMessageStore;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HelidonJpaOutboxMessageStoreProducerTest {

    @Test
    void createsTheExistingJpaOutboxStore() {
        OutboxMessageStore store = new HelidonJpaOutboxMessageStoreProducer().outboxMessageStore((EntityManager) null);

        assertThat(store).isInstanceOf(JpaOutboxMessageStore.class);
    }
}
