# Quarkus 运行时集成

`jfoundry-quarkus-runtime` 是一个 Quarkus 扩展，将运行时无关的 `TransactionRunner` 暴露为 CDI Bean。
它使 Quarkus、CDI、Jakarta Transactions 与 GraalVM 类型始终位于 domain、application 和
infrastructure 模块之外。

## 依赖配置

先导入当前 jfoundry 发布线的 Quarkus BOM，再添加 runtime 扩展。Quarkus 会通过 runtime 扩展描述符发现
deployment 构件；应用不应直接添加 deployment 构件。

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.github.xfoundries</groupId>
            <artifactId>jfoundry-quarkus-dependencies</artifactId>
            <version>${jfoundry.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>io.github.xfoundries</groupId>
        <artifactId>jfoundry-quarkus-runtime</artifactId>
    </dependency>
</dependencies>
```

该扩展在运行时引入 Quarkus Arc 与 Narayana JTA，并注册一个 application scope 的
`QuarkusTransactionRunner`，应用可通过运行时无关的 `TransactionRunner` 契约注入它。

## 事务语义

适配器将全部六种 `TransactionPropagation` 映射为 Jakarta Transactions 语义：

| jfoundry propagation | Quarkus/Jakarta 行为 |
|----------------------|----------------------|
| `REQUIRED` | 加入已有事务，或新建事务。 |
| `REQUIRES_NEW` | 挂起已有事务，新建事务，完成后恢复已有事务。 |
| `SUPPORTS` | 加入已有事务；没有事务时以非事务方式运行。 |
| `MANDATORY` | 必须存在活动事务。 |
| `NOT_SUPPORTED` | 挂起已有事务，并以非事务方式运行。 |
| `NEVER` | 仅在不存在活动事务时运行。 |

回调异常会回滚由适配器创建的事务。适配器加入已有事务时，回调异常会将该事务标记为 rollback-only，且
保留原始异常。

`TransactionOptions.timeout` 会映射为适配器创建事务所使用的 Jakarta 事务超时，并在结束后恢复默认值。
Jakarta Transactions 没有可移植的事务名称或只读事务设置，因此此适配器会拒绝
`TransactionOptions.name` 与 `TransactionOptions.readOnly`，而不会静默忽略它们。

## Native Image 验证

仓库的 Quarkus Native CI job 会先安装扩展构件，再构建独立的消费者应用。其
`@QuarkusIntegrationTest` 通过 HTTP 入口调用 `TransactionRunner`，针对原生可执行文件运行。

在安装了 GraalVM Native Image 的机器上，或 Docker 可用以进行 Quarkus 容器构建的机器上，可运行相同验证：

```bash
./mvnw -B \
  -pl jfoundry-quarkus/jfoundry-quarkus-runtime,jfoundry-quarkus/jfoundry-quarkus-deployment \
  -am -DskipTests install

./mvnw -B \
  -pl jfoundry-quarkus/jfoundry-quarkus-integration-tests \
  -Pnative -Dquarkus.native.container-build=true verify
```

## 当前范围

第一阶段 Quarkus 集成只覆盖 CDI 发现与应用事务。它尚未提供 JPA、MyBatis-Plus、Outbox、Inbox、消息、
调度、Web adapter、配置属性或 starter 的 Quarkus 装配；这些能力仍是后续的显式工作项。
