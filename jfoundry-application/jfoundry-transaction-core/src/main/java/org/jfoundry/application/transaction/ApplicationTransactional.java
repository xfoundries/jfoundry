package org.jfoundry.application.transaction;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an application-layer method or type as requiring a {@link TransactionRunner} boundary.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApplicationTransactional {

    /**
     * Optional transaction name.
     */
    String name() default "";

    /**
     * Whether the transaction should be read-only.
     */
    boolean readOnly() default false;

    /**
     * Transaction timeout in seconds. A non-positive value leaves the timeout unspecified.
     */
    long timeoutSeconds() default -1;

    /**
     * Transaction propagation behavior.
     */
    TransactionPropagation propagation() default TransactionPropagation.REQUIRED;
}
