package org.jfoundry.infrastructure.transaction.spring;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jfoundry.application.transaction.ApplicationTransactional;
import org.jfoundry.application.transaction.TransactionOptions;
import org.jfoundry.application.transaction.TransactionRunner;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Objects;

/**
 * Spring AOP interceptor that delegates {@link ApplicationTransactional} methods to {@link TransactionRunner}.
 */
public class ApplicationTransactionalInterceptor implements MethodInterceptor {

    private final TransactionRunner transactionRunner;

    public ApplicationTransactionalInterceptor(TransactionRunner transactionRunner) {
        this.transactionRunner = Objects.requireNonNull(transactionRunner, "transactionRunner must not be null");
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        ApplicationTransactional annotation = findAnnotation(invocation);
        if (annotation == null) {
            return invocation.proceed();
        }
        try {
            return transactionRunner.call(toOptions(annotation), () -> {
                try {
                    return invocation.proceed();
                } catch (Exception ex) {
                    throw ex;
                } catch (Throwable ex) {
                    throw new ThrowableInvocationException(ex);
                }
            });
        } catch (ThrowableInvocationException ex) {
            throw ex.getCause();
        }
    }

    private static ApplicationTransactional findAnnotation(MethodInvocation invocation) {
        Method method = invocation.getMethod();
        ApplicationTransactional annotation = method.getAnnotation(ApplicationTransactional.class);
        if (annotation != null) {
            return annotation;
        }
        Object target = invocation.getThis();
        if (target == null) {
            return null;
        }
        return target.getClass().getAnnotation(ApplicationTransactional.class);
    }

    private static TransactionOptions toOptions(ApplicationTransactional annotation) {
        TransactionOptions.Builder builder = TransactionOptions.builder()
                .name(annotation.name())
                .readOnly(annotation.readOnly())
                .propagation(annotation.propagation());
        if (annotation.timeoutSeconds() > 0) {
            builder.timeout(Duration.ofSeconds(annotation.timeoutSeconds()));
        }
        return builder.build();
    }

    private static final class ThrowableInvocationException extends RuntimeException {

        private ThrowableInvocationException(Throwable cause) {
            super(cause);
        }
    }
}
