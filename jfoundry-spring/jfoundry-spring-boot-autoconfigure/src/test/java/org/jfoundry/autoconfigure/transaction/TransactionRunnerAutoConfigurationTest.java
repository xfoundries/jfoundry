package org.jfoundry.autoconfigure.transaction;

import org.jfoundry.application.transaction.TransactionRunner;
import org.jfoundry.infrastructure.transaction.spring.SpringTransactionRunner;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class TransactionRunnerAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TransactionRunnerAutoConfiguration.class));

    @Test
    void createsSpringTransactionRunnerWhenTransactionManagerExists() {
        runner.withBean(PlatformTransactionManager.class, () -> mock(PlatformTransactionManager.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(TransactionRunner.class);
                    assertThat(context.getBean(TransactionRunner.class)).isInstanceOf(SpringTransactionRunner.class);
                });
    }

    @Test
    void backsOffWhenTransactionManagerIsMissing() {
        runner.run(context -> assertThat(context).doesNotHaveBean(TransactionRunner.class));
    }

    @Test
    void backsOffWhenUserProvidesTransactionRunner() {
        TransactionRunner userRunner = new TransactionRunner() {
            @Override
            public <T> T call(org.jfoundry.application.transaction.TransactionOptions options,
                              org.jfoundry.application.transaction.TransactionCallback<T> callback) throws Exception {
                return callback.execute();
            }
        };

        runner.withBean(PlatformTransactionManager.class, () -> mock(PlatformTransactionManager.class))
                .withBean(TransactionRunner.class, () -> userRunner)
                .run(context -> {
                    assertThat(context).hasSingleBean(TransactionRunner.class);
                    assertThat(context.getBean(TransactionRunner.class)).isSameAs(userRunner);
                });
    }
}
