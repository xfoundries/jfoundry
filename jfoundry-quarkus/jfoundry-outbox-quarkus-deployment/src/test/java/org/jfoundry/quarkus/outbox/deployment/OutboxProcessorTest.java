package org.jfoundry.quarkus.outbox.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import org.jfoundry.infrastructure.outbox.quarkus.QuarkusOutboxDispatcher;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxProcessorTest {

    @Test
    void registersTheOutboxDispatcher() {
        AdditionalBeanBuildItem beans = new OutboxProcessor().registerOutboxDispatcher();

        assertThat(beans.getBeanClasses()).contains(QuarkusOutboxDispatcher.class.getName());
        assertThat(beans.isRemovable()).isFalse();
    }
}
