package org.jfoundry.application.transaction;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationTransactionalTest {

    @Test
    void exposesApplicationTransactionMetadata() throws NoSuchMethodException {
        Method method = TransactionalService.class.getDeclaredMethod("handle");

        ApplicationTransactional annotation = method.getAnnotation(ApplicationTransactional.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.name()).isEqualTo("confirm-order");
        assertThat(annotation.readOnly()).isTrue();
        assertThat(annotation.timeoutSeconds()).isEqualTo(15);
        assertThat(annotation.propagation()).isEqualTo(TransactionPropagation.REQUIRES_NEW);
    }

    static class TransactionalService {

        @ApplicationTransactional(
                name = "confirm-order",
                readOnly = true,
                timeoutSeconds = 15,
                propagation = TransactionPropagation.REQUIRES_NEW)
        void handle() {
        }
    }
}
