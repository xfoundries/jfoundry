package org.jfoundry.application.outbox;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// P2-1: OutboxStatus must include DISPATCHING for atomic claim semantics.
class OutboxStatusTest {

    @Test
    void dispatchingStateExists() {
        assertThat(OutboxStatus.valueOf("DISPATCHING"))
                .isEqualTo(OutboxStatus.DISPATCHING);
    }

    @Test
    void hasFiveStates() {
        assertThat(OutboxStatus.values())
                .containsExactlyInAnyOrder(
                        OutboxStatus.PENDING,
                        OutboxStatus.DISPATCHING,
                        OutboxStatus.PUBLISHED,
                        OutboxStatus.FAILED,
                        OutboxStatus.DEAD_LETTERED);
    }
}
