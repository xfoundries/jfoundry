package org.jfoundry.autoconfigure.transaction;

import org.jfoundry.application.transaction.TransactionRunner;
import org.jfoundry.infrastructure.transaction.spring.ApplicationTransactionalInterceptor;
import org.jfoundry.infrastructure.transaction.spring.SpringTransactionRunner;
import org.junit.jupiter.api.Test;
import org.springframework.aop.Advisor;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class TransactionRunnerAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    TransactionRunnerAutoConfiguration.class,
                    ApplicationTransactionalAutoConfiguration.class));

    @Test
    void createsSpringTransactionRunnerWhenTransactionManagerExists() {
        runner.withBean(PlatformTransactionManager.class, () -> mock(PlatformTransactionManager.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(TransactionRunner.class);
                    assertThat(context.getBean(TransactionRunner.class)).isInstanceOf(SpringTransactionRunner.class);
                });
    }

    @Test
    void registersAdvisorForAutoConfiguredTransactionRunner() {
        runner.withBean(PlatformTransactionManager.class, () -> mock(PlatformTransactionManager.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(TransactionRunner.class);
                    assertThat(context).hasSingleBean(ApplicationTransactionalInterceptor.class);
                    assertThat(context).hasBean("applicationTransactionalAdvisor");
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

    @Test
    void registersApplicationTransactionalAdvisorWhenTransactionRunnerExists() {
        runner.withBean(TransactionRunner.class, () -> new TransactionRunner() {
                    @Override
                    public <T> T call(org.jfoundry.application.transaction.TransactionOptions options,
                                      org.jfoundry.application.transaction.TransactionCallback<T> callback) throws Exception {
                        return callback.execute();
                    }
                })
                .run(context -> {
                    assertThat(context).hasSingleBean(ApplicationTransactionalInterceptor.class);
                    assertThat(context).hasBean("applicationTransactionalAdvisor");
                    assertThat(context.getBean("applicationTransactionalAdvisor")).isInstanceOf(Advisor.class);
                });
    }

    @Test
    void canDisableApplicationTransactionalAdvisor() {
        runner.withBean(TransactionRunner.class, () -> new TransactionRunner() {
                    @Override
                    public <T> T call(org.jfoundry.application.transaction.TransactionOptions options,
                                      org.jfoundry.application.transaction.TransactionCallback<T> callback) throws Exception {
                        return callback.execute();
                    }
                })
                .withPropertyValues("jfoundry.application.transaction.annotation.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ApplicationTransactionalInterceptor.class);
                    assertThat(context).doesNotHaveBean("applicationTransactionalAdvisor");
                });
    }

    @Test
    void interceptsImplementationMethodAnnotationWhenBeanIsDeclaredByInterface() {
        CountingTransactionRunner transactionRunner = new CountingTransactionRunner();

        runner.withBean(TransactionRunner.class, () -> transactionRunner)
                .withBean(TransactionalService.class, AnnotatedTransactionalService::new)
                .run(context -> {
                    TransactionalService service = context.getBean(TransactionalService.class);

                    assertThat(AopUtils.isAopProxy(service)).isTrue();
                    service.execute();
                    assertThat(transactionRunner.invocationCount).isEqualTo(1);
                });
    }

    @Test
    void createsRunnerAfterBootAutoConfiguresDataSourceTransactionManager() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        TransactionRunnerAutoConfiguration.class,
                        ApplicationTransactionalAutoConfiguration.class,
                        DataSourceAutoConfiguration.class,
                        DataSourceTransactionManagerAutoConfiguration.class,
                        TransactionAutoConfiguration.class))
                .withPropertyValues("spring.datasource.url=jdbc:h2:mem:jfoundry-transaction-ordering")
                .withBean(TransactionalService.class, AnnotatedTransactionalService::new)
                .run(context -> {
                    assertThat(context).hasSingleBean(PlatformTransactionManager.class);
                    assertThat(context).hasSingleBean(TransactionRunner.class);
                    assertThat(AopUtils.isAopProxy(context.getBean(TransactionalService.class))).isTrue();
                });
    }

    interface TransactionalService {

        void execute();
    }

    static class AnnotatedTransactionalService implements TransactionalService {

        @org.jfoundry.application.transaction.ApplicationTransactional
        @Override
        public void execute() {
        }
    }

    static class CountingTransactionRunner implements TransactionRunner {

        private int invocationCount;

        @Override
        public <T> T call(org.jfoundry.application.transaction.TransactionOptions options,
                          org.jfoundry.application.transaction.TransactionCallback<T> callback) throws Exception {
            invocationCount++;
            return callback.execute();
        }
    }
}
