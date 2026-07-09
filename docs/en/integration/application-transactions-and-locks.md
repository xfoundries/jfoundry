# Application Transactions and Distributed Locks

This page explains two application-layer boundary tools: `@ApplicationTransactional` and
distributed locks. Both are optional entry points over framework-neutral contracts. The annotations
are conveniences for Spring applications; the core APIs remain independent of Spring and Redisson.

## Application Transactions

`TransactionRunner` is the primary contract. Use it when application orchestration needs an explicit
transaction block without depending on Spring `TransactionTemplate`:

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

Disable the annotation advisor when needed:

```properties
jfoundry.application.transaction.annotation.enabled=false
```

## Distributed Locks

Distributed lock support has a framework-neutral core and explicit Redisson integration:

- `jfoundry-lock-core`: `DistributedLockClient`, `LockTemplate`, `LockOptions`, and `@DistributedLock`.
- `jfoundry-lock-redisson`: Redisson `RLock` adapter.
- `jfoundry-lock-spring`: Spring AOP interceptor for `@DistributedLock`.
- `jfoundry-lock-redisson-spring-boot-starter`: explicit Spring Boot entry point.

Programmatic usage:

```java
lockTemplate.execute(
        "order:" + command.orderId(),
        LockOptions.builder()
                .waitTime(Duration.ofSeconds(2))
                .leaseTime(Duration.ofSeconds(10))
                .build(),
        () -> {
            applicationService.confirm(command);
            return null;
        });
```

Annotation usage:

```java
@DistributedLock(key = "'order:' + #command.orderId", waitTime = "2s", leaseTime = "10s")
public void confirm(ConfirmOrderCommand command) {
    // application orchestration
}
```

The `key` attribute accepts a plain lock name, such as `order:daily-rebuild`, or an explicit
Spring Expression Language expression, such as `"'order:' + #command.orderId"`. Parameters are
available by name when compiled with `-parameters`; `#p0` and `#a0` style aliases are always
available.

`waitTime` accepts `ms`, `s`, `m`, `h`, or ISO-8601 duration text such as `PT10S`. Empty
`leaseTime` delegates expiration behavior to the lock backend. For Redisson this means using its
watchdog behavior; setting `leaseTime` uses Redisson's timed `tryLock(waitTime, leaseTime, unit)`.

When the lock cannot be acquired, the default `LockFailureMode.THROW` raises
`DistributedLockUnavailableException`. Use `failureMode = LockFailureMode.SKIP` only when skipping
the method is an intentional business outcome.

Spring Boot dependency:

```xml
<dependency>
    <groupId>io.github.xfoundries</groupId>
    <artifactId>jfoundry-lock-redisson-spring-boot-starter</artifactId>
</dependency>
```

Disable the annotation advisor when needed:

```properties
jfoundry.lock.annotation.enabled=false
```

## Ordering

When both `@DistributedLock` and `@ApplicationTransactional` apply to the same method, the lock
advisor runs first and the transaction advisor runs inside the lock. This avoids opening a database
transaction while waiting for a distributed lock.
