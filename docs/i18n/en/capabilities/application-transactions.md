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

Runtime integrations can expose declarative transaction boundaries, but they do not replace a
runtime's infrastructure-level transaction mechanism. Keep the application transaction boundary at
the use-case or application service boundary.

When a method also needs a distributed lock, see [Distributed Locks](distributed-locks.md) for the
advisor ordering.

For runtime assembly, user overrides, and annotation configuration, see
[Spring Boot Runtime Assembly](../implementations/spring-boot.md). The exact starter, property, and
auto-configuration conditions are in the
[Spring Boot Auto-configuration reference](../reference/spring-boot-autoconfiguration.md).
