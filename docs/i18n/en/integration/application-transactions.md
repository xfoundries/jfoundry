# Application Transactions

`TransactionRunner` is jfoundry's primary contract for application-layer transaction boundaries. Use
it when application orchestration needs an explicit transaction block without depending on Spring
`TransactionTemplate`:

```java
transactionRunner.run(TransactionOptions.builder()
        .name("confirm-order")
        .timeout(Duration.ofSeconds(10))
        .build(), () -> {
    orderRepository.save(order);
    outboxRecorder.record(events);
});
```

Spring projects can also use `@ApplicationTransactional` on application services:

```java
@ApplicationTransactional(name = "confirm-order", timeoutSeconds = 10)
public void confirm(ConfirmOrderCommand command) {
    // application orchestration
}
```

The annotation is implemented by Spring AOP and delegates to `TransactionRunner`. It is not a
replacement for Spring `@Transactional` in infrastructure code. Use it at use-case or application
service boundaries when you want the application layer to express the transaction boundary through
jfoundry's contract.

Spring Boot projects get this integration through `jfoundry-spring-boot-starter`. When a
`PlatformTransactionManager` is available, auto-configuration creates a `SpringTransactionRunner`.
The annotation advisor is enabled by default when a `TransactionRunner` bean exists.

Disable the annotation advisor when needed:

```properties
jfoundry.application.transaction.annotation.enabled=false
```

When a method also needs a distributed lock, see [Distributed Locks](distributed-locks.md) for the
advisor ordering.
