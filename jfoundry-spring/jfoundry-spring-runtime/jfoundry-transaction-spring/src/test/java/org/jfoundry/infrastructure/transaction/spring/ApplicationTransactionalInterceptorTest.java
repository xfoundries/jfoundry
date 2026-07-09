package org.jfoundry.infrastructure.transaction.spring;

import org.aopalliance.intercept.MethodInvocation;
import org.jfoundry.application.transaction.ApplicationTransactional;
import org.jfoundry.application.transaction.TransactionCallback;
import org.jfoundry.application.transaction.TransactionOptions;
import org.jfoundry.application.transaction.TransactionPropagation;
import org.jfoundry.application.transaction.TransactionRunner;
import org.junit.jupiter.api.Test;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationTransactionalInterceptorTest {

    @Test
    void delegatesAnnotatedMethodToTransactionRunner() throws Throwable {
        RecordingTransactionRunner runner = new RecordingTransactionRunner();
        ApplicationTransactionalInterceptor interceptor = new ApplicationTransactionalInterceptor(runner);
        Method method = TransactionalService.class.getDeclaredMethod("handle");

        Object result = interceptor.invoke(new SimpleMethodInvocation(new TransactionalService(), method));

        assertThat(result).isEqualTo("handled");
        assertThat(runner.options.name()).contains("confirm-order");
        assertThat(runner.options.readOnly()).isTrue();
        assertThat(runner.options.timeout()).contains(Duration.ofSeconds(15));
        assertThat(runner.options.propagation()).isEqualTo(TransactionPropagation.REQUIRES_NEW);
        assertThat(runner.callCount).isEqualTo(1);
    }

    static class TransactionalService {

        @ApplicationTransactional(
                name = "confirm-order",
                readOnly = true,
                timeoutSeconds = 15,
                propagation = TransactionPropagation.REQUIRES_NEW)
        String handle() {
            return "handled";
        }
    }

    static class RecordingTransactionRunner implements TransactionRunner {

        private TransactionOptions options;
        private int callCount;

        @Override
        public <T> T call(TransactionOptions options, TransactionCallback<T> callback) throws Exception {
            this.options = options;
            this.callCount++;
            return callback.execute();
        }
    }

    record SimpleMethodInvocation(Object target, Method method) implements MethodInvocation {

        @Override
        public Object proceed() throws Throwable {
            return method.invoke(target);
        }

        @Override
        public Method getMethod() {
            return method;
        }

        @Override
        public Object[] getArguments() {
            return new Object[0];
        }

        @Override
        public Object getThis() {
            return target;
        }

        @Override
        public AccessibleObject getStaticPart() {
            return method;
        }
    }
}
