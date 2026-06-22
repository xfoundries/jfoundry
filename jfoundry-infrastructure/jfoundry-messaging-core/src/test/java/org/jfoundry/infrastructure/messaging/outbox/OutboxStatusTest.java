package org.jfoundry.infrastructure.messaging.outbox;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxStatusTest {

    @Test
    void containsAllFourStates() {
        assertThat(OutboxStatus.values())
                .containsExactlyInAnyOrder(
                        OutboxStatus.PENDING,
                        OutboxStatus.PUBLISHED,
                        OutboxStatus.FAILED,
                        OutboxStatus.DEAD_LETTERED);
    }
}
