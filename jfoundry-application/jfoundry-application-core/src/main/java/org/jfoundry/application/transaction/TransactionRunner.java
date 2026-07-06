package org.jfoundry.application.transaction;

/**
 * Runs application code inside an explicit transaction boundary.
 */
public interface TransactionRunner {

    default void run(TransactionAction action) throws Exception {
        run(TransactionOptions.defaults(), action);
    }

    default <T> T call(TransactionCallback<T> callback) throws Exception {
        return call(TransactionOptions.defaults(), callback);
    }

    default void run(TransactionOptions options, TransactionAction action) throws Exception {
        call(options, () -> {
            action.execute();
            return null;
        });
    }

    <T> T call(TransactionOptions options, TransactionCallback<T> callback) throws Exception;
}
