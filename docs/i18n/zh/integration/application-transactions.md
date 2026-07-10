# 应用事务

`TransactionRunner` 是 jfoundry 的应用层事务边界契约。当应用编排需要显式事务代码块，但不希望依赖 Spring `TransactionTemplate` 时，可以直接使用：

```java
transactionRunner.run(TransactionOptions.builder()
        .name("confirm-order")
        .timeout(Duration.ofSeconds(10))
        .build(), () -> {
    orderRepository.save(order);
    outboxRecorder.record(events);
});
```

Spring 项目也可以在应用服务上使用 `@ApplicationTransactional`：

```java
@ApplicationTransactional(name = "confirm-order", timeoutSeconds = 10)
public void confirm(ConfirmOrderCommand command) {
    // application orchestration
}
```

该注解由 Spring AOP 实现，并委托给 `TransactionRunner`。它不是基础设施代码中 Spring `@Transactional` 的替代品。建议在 Use Case 或 Application Service 边界使用，让应用层通过 jfoundry 契约表达事务边界。

Spring Boot 项目通过 `jfoundry-spring-boot-starter` 获得该集成。当存在 `PlatformTransactionManager` 时，自动配置会创建 `SpringTransactionRunner`。存在 `TransactionRunner` Bean 时，注解 advisor 默认开启。

如需关闭注解 advisor：

```properties
jfoundry.application.transaction.annotation.enabled=false
```

如果同一方法还需要分布式锁，advisor 执行顺序见 [分布式锁](distributed-locks.md)。
