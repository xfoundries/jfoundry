package org.jfoundry.infrastructure.lock.spring;

import org.aopalliance.intercept.MethodInvocation;
import org.jfoundry.application.lock.DistributedLock;
import org.jfoundry.application.lock.LockCallback;
import org.jfoundry.application.lock.LockOptions;
import org.jfoundry.application.lock.LockTemplate;
import org.junit.jupiter.api.Test;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class DistributedLockInterceptorTest {

    @Test
    void resolvesSpelKeyAndDelegatesToLockTemplate() throws Throwable {
        RecordingLockTemplate lockTemplate = new RecordingLockTemplate();
        DistributedLockInterceptor interceptor = new DistributedLockInterceptor(lockTemplate);
        Method method = LockedService.class.getDeclaredMethod("handle", String.class);

        Object result = interceptor.invoke(new SimpleMethodInvocation(new LockedService(), method, new Object[]{"42"}));

        assertThat(result).isEqualTo("handled:42");
        assertThat(lockTemplate.name).isEqualTo("order:42");
        assertThat(lockTemplate.options.waitTime()).contains(java.time.Duration.ofSeconds(2));
        assertThat(lockTemplate.options.leaseTime()).contains(java.time.Duration.ofSeconds(10));
    }

    @Test
    void treatsPlainKeyAsLockName() throws Throwable {
        RecordingLockTemplate lockTemplate = new RecordingLockTemplate();
        DistributedLockInterceptor interceptor = new DistributedLockInterceptor(lockTemplate);
        Method method = LockedService.class.getDeclaredMethod("handlePlain");

        Object result = interceptor.invoke(new SimpleMethodInvocation(new LockedService(), method, new Object[0]));

        assertThat(result).isEqualTo("handled");
        assertThat(lockTemplate.name).isEqualTo("order:plain");
    }

    static class LockedService {

        @DistributedLock(key = "'order:' + #orderId", waitTime = "2s", leaseTime = "10s")
        String handle(String orderId) {
            return "handled:" + orderId;
        }

        @DistributedLock(key = "order:plain")
        String handlePlain() {
            return "handled";
        }
    }

    static class RecordingLockTemplate extends LockTemplate {

        private String name;
        private LockOptions options;

        RecordingLockTemplate() {
            super((name, options) -> new org.jfoundry.application.lock.LockHandle(name, true));
        }

        @Override
        public <T> T execute(String name, LockOptions options, LockCallback<T> callback) throws Exception {
            this.name = name;
            this.options = options;
            return callback.execute();
        }
    }

    record SimpleMethodInvocation(Object target, Method method, Object[] arguments) implements MethodInvocation {

        @Override
        public Object proceed() throws Throwable {
            return method.invoke(target, arguments);
        }

        @Override
        public Method getMethod() {
            return method;
        }

        @Override
        public Object[] getArguments() {
            return arguments;
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
