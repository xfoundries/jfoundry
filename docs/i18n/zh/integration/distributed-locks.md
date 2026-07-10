# 分布式锁

分布式锁由框架无关 core 和显式 Redisson 集成组成：

- `jfoundry-lock-core`：`DistributedLockClient`、`LockTemplate`、`LockOptions`、`@DistributedLock`。
- `jfoundry-lock-redisson`：Redisson `RLock` adapter。
- `jfoundry-lock-spring`：`@DistributedLock` 的 Spring AOP 拦截器。
- `jfoundry-lock-redisson-spring-boot-starter`：显式 Spring Boot 入口。

编程式用法：

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

注解用法：

```java
@DistributedLock(key = "'order:' + #command.orderId", waitTime = "2s", leaseTime = "10s")
public void confirm(ConfirmOrderCommand command) {
    // application orchestration
}
```

`key` 可以是普通锁名，例如 `order:daily-rebuild`；也可以是显式 Spring Expression Language 表达式，例如 `"'order:' + #command.orderId"`。使用 `-parameters` 编译时可以按参数名引用参数；`#p0` 和 `#a0` 这类别名始终可用。

`waitTime` 支持 `ms`、`s`、`m`、`h`，也支持 `PT10S` 这类 ISO-8601 duration。`leaseTime` 为空时把过期行为交给锁后端。对 Redisson 而言，这意味着使用 watchdog；设置 `leaseTime` 时使用 Redisson 的定时 `tryLock(waitTime, leaseTime, unit)`。

无法获得锁时，默认 `LockFailureMode.THROW` 会抛出 `DistributedLockUnavailableException`。只有当“跳过执行”本身就是明确业务结果时，才使用 `failureMode = LockFailureMode.SKIP`。

Spring Boot 依赖：

```xml
<dependency>
    <groupId>io.github.xfoundries</groupId>
    <artifactId>jfoundry-lock-redisson-spring-boot-starter</artifactId>
</dependency>
```

如需关闭注解 advisor：

```properties
jfoundry.lock.annotation.enabled=false
```

## 与事务的执行顺序

当同一方法同时使用 `@DistributedLock` 和 `@ApplicationTransactional` 时，锁 advisor 先执行，事务 advisor 在锁内执行。这样可以避免在等待分布式锁期间提前打开数据库事务。
