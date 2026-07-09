package org.jfoundry.application.lock;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LockTemplateTest {

    @Test
    void executesCallbackWhenLockIsAcquiredAndReleasesAfterwards() throws Exception {
        RecordingLockClient client = new RecordingLockClient(true);
        LockTemplate template = new LockTemplate(client);

        String result = template.execute("order:1", LockOptions.defaults(), () -> "handled");

        assertThat(result).isEqualTo("handled");
        assertThat(client.events).containsExactly("try:order:1", "release:order:1");
    }

    @Test
    void releasesLockWhenCallbackFails() {
        RecordingLockClient client = new RecordingLockClient(true);
        LockTemplate template = new LockTemplate(client);

        assertThatThrownBy(() -> template.execute("order:1", LockOptions.defaults(), () -> {
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");

        assertThat(client.events).containsExactly("try:order:1", "release:order:1");
    }

    @Test
    void throwsWhenLockCannotBeAcquired() {
        RecordingLockClient client = new RecordingLockClient(false);
        LockTemplate template = new LockTemplate(client);
        LockOptions options = LockOptions.builder()
                .waitTime(Duration.ofMillis(10))
                .leaseTime(Duration.ofSeconds(5))
                .failureMode(LockFailureMode.THROW)
                .build();

        assertThatThrownBy(() -> template.execute("order:1", options, () -> "ignored"))
                .isInstanceOf(DistributedLockUnavailableException.class)
                .hasMessageContaining("order:1");

        assertThat(client.events).containsExactly("try:order:1");
    }

    @Test
    void skipsCallbackWhenLockCannotBeAcquiredAndFailureModeIsSkip() throws Exception {
        RecordingLockClient client = new RecordingLockClient(false);
        LockTemplate template = new LockTemplate(client);
        LockOptions options = LockOptions.builder()
                .failureMode(LockFailureMode.SKIP)
                .build();

        String result = template.execute("order:1", options, () -> "ignored");

        assertThat(result).isNull();
        assertThat(client.events).containsExactly("try:order:1");
    }

    static class RecordingLockClient implements DistributedLockClient {

        private final boolean acquired;
        private final List<String> events = new ArrayList<>();

        RecordingLockClient(boolean acquired) {
            this.acquired = acquired;
        }

        @Override
        public LockHandle tryLock(String name, LockOptions options) {
            events.add("try:" + name);
            return new LockHandle(name, acquired, () -> events.add("release:" + name));
        }
    }
}
