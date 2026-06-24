package org.jfoundry.application.outbox;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// P2-1: OutboxMessageStatus must include DISPATCHING for atomic claim semantics.
class OutboxMessageStatusTest {

    @Test
    void dispatchingStateExists() {
        assertThat(OutboxMessageStatus.valueOf("DISPATCHING"))
                .isEqualTo(OutboxMessageStatus.DISPATCHING);
    }

    @Test
    void hasFiveStates() {
        assertThat(OutboxMessageStatus.values())
                .containsExactlyInAnyOrder(
                        OutboxMessageStatus.PENDING,
                        OutboxMessageStatus.DISPATCHING,
                        OutboxMessageStatus.PUBLISHED,
                        OutboxMessageStatus.FAILED,
                        OutboxMessageStatus.DEAD_LETTERED);
    }
}
