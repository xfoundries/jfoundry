# Transactional Outbox Completion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete jfoundry's Transactional Outbox runtime with broker-neutral aggregate metadata, a Kafka sender adapter, and consumer-side Inbox idempotency.

**Architecture:** Keep outbox core broker-neutral and extend it only with aggregate ordering metadata. Add Kafka as a `MessageSender` adapter in its own messaging module. Add Inbox as a separate consumer-side reliability capability with MyBatis-Plus persistence and explicit `InboxTemplate.executeOnce(...)` usage.

**Tech Stack:** Java 21, Maven multi-module, Spring Boot 3.2.7 auto-configuration, MyBatis-Plus 3.5.12, Spring Kafka, JUnit Jupiter, AssertJ, Mockito, H2.

---

## Important Current Workspace Note

The user intentionally changed `OutboxData.@TableName` from `ddd_outbox_event` to `jfoundry_outbox_event`. Treat this as an intended compatibility change, not an accidental dirty file. Implementation must align the default outbox table name, migration scripts, docs, and tests around `jfoundry_outbox_event`, while preserving configurable table override through `jfoundry.outbox.table-name`.

## File Structure

### Existing Files To Modify

- `pom.xml`: no direct module changes unless Maven reactor ordering requires it.
- `jfoundry-dependencies/pom.xml`: add managed dependencies for new modules and Spring Kafka artifacts.
- `jfoundry-infrastructure/pom.xml`: add `jfoundry-messaging-kafka`, `jfoundry-inbox-core`, and `jfoundry-inbox-mybatis-plus` modules.
- `jfoundry-spring/jfoundry-spring-boot-starter/pom.xml`: add Inbox runtime dependencies; keep Kafka adapter out of the default starter so non-Kafka users do not receive Spring Kafka transitively.
- `jfoundry-spring/jfoundry-autoconfigure/pom.xml`: add dependencies on new modules; keep Spring Kafka classes optional/provided so Kafka auto-configuration is conditional on user classpath.
- `jfoundry-spring/jfoundry-autoconfigure/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`: update package names and add Kafka/Inbox auto-configurations.
- `jfoundry-infrastructure/jfoundry-outbox-core/src/main/java/org/jfoundry/infrastructure/outbox/core/OutboxEntry.java`: add aggregate fields and a metadata-aware factory.
- `jfoundry-infrastructure/jfoundry-outbox-mybatis-plus/src/main/java/org/jfoundry/infrastructure/outbox/mybatis/OutboxData.java`: keep `@TableName("jfoundry_outbox_event")`, add aggregate fields, map entry/data both ways.
- `jfoundry-infrastructure/jfoundry-outbox-mybatis-plus/src/main/resources/db/migration/V20260617__create_outbox_event.sql`: update default table name and aggregate columns.
- `jfoundry-infrastructure/jfoundry-outbox-mybatis-plus/src/main/resources/db/migration/V20260617__create_outbox_event_dm.sql`: update default table name and aggregate columns.
- `jfoundry-infrastructure/jfoundry-outbox-mybatis-plus/src/test/resources/outbox_event.sql`: update default table name and aggregate columns.
- `jfoundry-spring/jfoundry-autoconfigure/src/test/resources/outbox_event.sql`: update default table name and aggregate columns.
- `jfoundry-spring/jfoundry-autoconfigure/src/test/resources/outbox_custom_table.sql`: mirror aggregate columns.
- `jfoundry-infrastructure/jfoundry-outbox-spring/src/main/java/org/jfoundry/infrastructure/outbox/spring/externalization/DomainEventExternalizer.java`: resolve aggregate metadata and choose fallback payload key.
- `jfoundry-infrastructure/jfoundry-messaging-core/src/main/java/org/jfoundry/infrastructure/messaging/externalization/ExternalizationRuleResolver.java`: reuse or extract property path evaluation for aggregate metadata.
- `README.md`: update module list and usage docs.
- `docs/transactional-outbox.md`: document polling relay, Kafka sender, aggregate metadata, Inbox idempotency, and CDC out of scope.
- `docs/framework-boundaries.md`: update modules and package boundary notes.

### Existing Files To Move/Rename By Package

- Move `jfoundry-spring/jfoundry-autoconfigure/src/main/java/org/jfoundry/autoconfigure/persistence/JfoundryOutboxProperties.java` to package `org.jfoundry.autoconfigure.outbox`.
- Move `jfoundry-spring/jfoundry-autoconfigure/src/main/java/org/jfoundry/autoconfigure/persistence/OutboxMybatisPlusAutoConfiguration.java` to package `org.jfoundry.autoconfigure.outbox.persistence`.
- Move outbox dispatcher auto-configuration and job classes from package `org.jfoundry.autoconfigure.dispatcher` to `org.jfoundry.autoconfigure.outbox.dispatcher`.
- Update tests under `jfoundry-spring/jfoundry-autoconfigure/src/test/java` to match new packages.

### New Files To Create

- `jfoundry-infrastructure/jfoundry-messaging-kafka/pom.xml`
- `jfoundry-infrastructure/jfoundry-messaging-kafka/src/main/java/org/jfoundry/infrastructure/messaging/kafka/KafkaMessageSender.java`
- `jfoundry-infrastructure/jfoundry-messaging-kafka/src/test/java/org/jfoundry/infrastructure/messaging/kafka/KafkaMessageSenderTest.java`
- `jfoundry-infrastructure/jfoundry-inbox-core/pom.xml`
- `jfoundry-infrastructure/jfoundry-inbox-core/src/main/java/org/jfoundry/infrastructure/inbox/InboxHandler.java`
- `jfoundry-infrastructure/jfoundry-inbox-core/src/main/java/org/jfoundry/infrastructure/inbox/InboxMessage.java`
- `jfoundry-infrastructure/jfoundry-inbox-core/src/main/java/org/jfoundry/infrastructure/inbox/InboxMessageStatus.java`
- `jfoundry-infrastructure/jfoundry-inbox-core/src/main/java/org/jfoundry/infrastructure/inbox/InboxRepository.java`
- `jfoundry-infrastructure/jfoundry-inbox-core/src/main/java/org/jfoundry/infrastructure/inbox/InboxTemplate.java`
- `jfoundry-infrastructure/jfoundry-inbox-core/src/test/java/org/jfoundry/infrastructure/inbox/InboxTemplateTest.java`
- `jfoundry-infrastructure/jfoundry-inbox-mybatis-plus/pom.xml`
- `jfoundry-infrastructure/jfoundry-inbox-mybatis-plus/src/main/java/org/jfoundry/infrastructure/inbox/mybatis/InboxMessageData.java`
- `jfoundry-infrastructure/jfoundry-inbox-mybatis-plus/src/main/java/org/jfoundry/infrastructure/inbox/mybatis/InboxMessageMapper.java`
- `jfoundry-infrastructure/jfoundry-inbox-mybatis-plus/src/main/java/org/jfoundry/infrastructure/inbox/mybatis/MybatisPlusInboxRepository.java`
- `jfoundry-infrastructure/jfoundry-inbox-mybatis-plus/src/main/resources/db/migration/V20260624__create_inbox_message.sql`
- `jfoundry-infrastructure/jfoundry-inbox-mybatis-plus/src/test/resources/inbox_message.sql`
- `jfoundry-infrastructure/jfoundry-inbox-mybatis-plus/src/test/java/org/jfoundry/infrastructure/inbox/mybatis/InboxPersistenceTestConfig.java`
- `jfoundry-infrastructure/jfoundry-inbox-mybatis-plus/src/test/java/org/jfoundry/infrastructure/inbox/mybatis/MybatisPlusInboxRepositoryTest.java`
- `jfoundry-infrastructure/jfoundry-messaging-core/src/main/java/org/jfoundry/infrastructure/messaging/externalization/AggregateRouting.java`
- `jfoundry-infrastructure/jfoundry-messaging-core/src/main/java/org/jfoundry/infrastructure/messaging/externalization/AggregateRoutingMetadata.java`
- `jfoundry-infrastructure/jfoundry-messaging-core/src/main/java/org/jfoundry/infrastructure/messaging/externalization/AggregateRoutingResolver.java`
- `jfoundry-spring/jfoundry-autoconfigure/src/main/java/org/jfoundry/autoconfigure/messaging/kafka/KafkaMessageSenderAutoConfiguration.java`
- `jfoundry-spring/jfoundry-autoconfigure/src/main/java/org/jfoundry/autoconfigure/messaging/kafka/KafkaMessageSenderProperties.java`
- `jfoundry-spring/jfoundry-autoconfigure/src/main/java/org/jfoundry/autoconfigure/inbox/InboxAutoConfiguration.java`
- `jfoundry-spring/jfoundry-autoconfigure/src/main/java/org/jfoundry/autoconfigure/inbox/InboxMybatisPlusAutoConfiguration.java`
- `jfoundry-spring/jfoundry-autoconfigure/src/test/java/org/jfoundry/autoconfigure/messaging/kafka/KafkaMessageSenderAutoConfigurationTest.java`
- `jfoundry-spring/jfoundry-autoconfigure/src/test/java/org/jfoundry/autoconfigure/inbox/InboxAutoConfigurationTest.java`

---

## Task 1: Rename Outbox Auto-Configuration Packages

**Files:**
- Move: `jfoundry-spring/jfoundry-autoconfigure/src/main/java/org/jfoundry/autoconfigure/persistence/JfoundryOutboxProperties.java`
- Move: `jfoundry-spring/jfoundry-autoconfigure/src/main/java/org/jfoundry/autoconfigure/persistence/OutboxMybatisPlusAutoConfiguration.java`
- Move: `jfoundry-spring/jfoundry-autoconfigure/src/main/java/org/jfoundry/autoconfigure/dispatcher/*.java`
- Modify: `jfoundry-spring/jfoundry-autoconfigure/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Modify affected tests under `jfoundry-spring/jfoundry-autoconfigure/src/test/java`

- [ ] **Step 1: Write/adjust package expectation through compilation**

Update imports in one existing test first, for example `OutboxTableNameOverrideTest`, to import `org.jfoundry.autoconfigure.outbox.persistence.OutboxMybatisPlusAutoConfiguration`.

Run:

```bash
mvn -pl jfoundry-spring/jfoundry-autoconfigure -am -Dtest=OutboxTableNameOverrideTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: FAIL at compilation because the new package does not exist yet.

- [ ] **Step 2: Move classes and update package declarations**

Move source files to:

```text
jfoundry-spring/jfoundry-autoconfigure/src/main/java/org/jfoundry/autoconfigure/outbox/JfoundryOutboxProperties.java
jfoundry-spring/jfoundry-autoconfigure/src/main/java/org/jfoundry/autoconfigure/outbox/persistence/OutboxMybatisPlusAutoConfiguration.java
jfoundry-spring/jfoundry-autoconfigure/src/main/java/org/jfoundry/autoconfigure/outbox/dispatcher/*.java
```

Set package declarations:

```java
package org.jfoundry.autoconfigure.outbox;
package org.jfoundry.autoconfigure.outbox.persistence;
package org.jfoundry.autoconfigure.outbox.dispatcher;
```

Update imports from old packages to new packages.

- [ ] **Step 3: Update auto-configuration imports**

Change `AutoConfiguration.imports` to:

```text
org.jfoundry.autoconfigure.messaging.DomainEventPublisherAutoConfiguration
org.jfoundry.autoconfigure.messaging.MessageSenderAutoConfiguration
org.jfoundry.autoconfigure.messaging.DomainEventExternalizerAutoConfiguration
org.jfoundry.autoconfigure.outbox.persistence.OutboxMybatisPlusAutoConfiguration
org.jfoundry.autoconfigure.outbox.dispatcher.OutboxDispatcherAutoConfiguration
org.jfoundry.autoconfigure.outbox.dispatcher.JobRunrDispatcherAutoConfiguration
```

- [ ] **Step 4: Run package-move tests**

Run:

```bash
mvn -pl jfoundry-spring/jfoundry-autoconfigure -am test
```

Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add jfoundry-spring/jfoundry-autoconfigure/src/main/java jfoundry-spring/jfoundry-autoconfigure/src/test/java jfoundry-spring/jfoundry-autoconfigure/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
git commit -m "refactor(outbox): move auto-configuration under outbox package"
```

---

## Task 2: Stabilize Outbox Table Name Override and Default Table Name

**Files:**
- Modify: `jfoundry-spring/jfoundry-autoconfigure/src/main/java/org/jfoundry/autoconfigure/outbox/JfoundryOutboxProperties.java`
- Modify: `jfoundry-spring/jfoundry-autoconfigure/src/main/java/org/jfoundry/autoconfigure/outbox/persistence/OutboxMybatisPlusAutoConfiguration.java`
- Create: `jfoundry-spring/jfoundry-autoconfigure/src/main/java/org/jfoundry/autoconfigure/outbox/persistence/OutboxTableNameCustomizer.java`
- Modify: `jfoundry-infrastructure/jfoundry-outbox-mybatis-plus/src/main/java/org/jfoundry/infrastructure/outbox/mybatis/OutboxData.java`
- Modify: migration and test SQL files using `ddd_outbox_event`
- Modify: `OutboxTableNameOverrideTest`

- [ ] **Step 1: Write failing test for user-provided MybatisPlusInterceptor**

Add a test class:

```text
jfoundry-spring/jfoundry-autoconfigure/src/test/java/org/jfoundry/autoconfigure/outbox/persistence/OutboxTableNameWithCustomInterceptorTest.java
```

Test scenario:

```java
@SpringBootTest(
        classes = OutboxTableNameWithCustomInterceptorTest.TestApp.class,
        properties = {
                "jfoundry.outbox.table-name=custom_outbox",
                "jfoundry.outbox.dispatcher.enabled=false",
                "spring.datasource.url=jdbc:h2:mem:jfoundry-custom-interceptor;DB_CLOSE_DELAY=-1",
                "spring.sql.init.schema-locations=classpath:outbox_event.sql"
        }
)
@Sql(scripts = "classpath:outbox_custom_table.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class OutboxTableNameWithCustomInterceptorTest {
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @MapperScan(basePackages = "org.jfoundry.infrastructure.outbox.mybatis")
    static class TestApp {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        MybatisPlusInterceptor mybatisPlusInterceptor() {
            MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
            interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.H2));
            return interceptor;
        }
    }

    @Autowired OutboxRepository repository;
    @Autowired JdbcTemplate jdbc;

    @Test
    void customInterceptorStillReceivesOutboxTableRewrite() {
        OutboxEntry entry = OutboxEntry.newPending(
                "evt-custom-interceptor", "test.event", null, "test.type", "{}", Instant.now());

        repository.append(entry);

        Integer customCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM custom_outbox WHERE event_id = ?",
                Integer.class, "evt-custom-interceptor");
        Integer defaultCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM jfoundry_outbox_event WHERE event_id = ?",
                Integer.class, "evt-custom-interceptor");

        assertThat(customCount).isEqualTo(1);
        assertThat(defaultCount).isEqualTo(0);
    }
}
```

Run:

```bash
mvn -pl jfoundry-spring/jfoundry-autoconfigure -am -Dtest=OutboxTableNameWithCustomInterceptorTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: FAIL because the user-provided interceptor does not have the outbox dynamic table handler.

- [ ] **Step 2: Implement reusable table-name customizer**

Create `OutboxTableNameCustomizer`:

```java
package org.jfoundry.autoconfigure.outbox.persistence;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TableNameHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.DynamicTableNameInnerInterceptor;
import org.jfoundry.autoconfigure.outbox.JfoundryOutboxProperties;

public final class OutboxTableNameCustomizer {

    public static final String OUTBOX_LOGICAL_TABLE = "jfoundry_outbox_event";

    private final JfoundryOutboxProperties properties;

    public OutboxTableNameCustomizer(JfoundryOutboxProperties properties) {
        this.properties = properties;
    }

    public void customize(MybatisPlusInterceptor interceptor) {
        interceptor.addInnerInterceptor(dynamicTableNameInnerInterceptor());
    }

    private DynamicTableNameInnerInterceptor dynamicTableNameInnerInterceptor() {
        TableNameHandler handler = (sqlStatement, tableName) ->
                OUTBOX_LOGICAL_TABLE.equals(tableName) ? properties.getTableName() : tableName;
        return new DynamicTableNameInnerInterceptor(handler);
    }
}
```

- [ ] **Step 3: Apply customizer to default and user-provided interceptors**

In `OutboxMybatisPlusAutoConfiguration`:

- register `OutboxTableNameCustomizer` as a bean;
- call it when creating the default `MybatisPlusInterceptor`;
- add a `BeanPostProcessor` that customizes any user-provided `MybatisPlusInterceptor` if it is not already customized.

Use a private marker set based on interceptor identity to avoid double-adding the handler.

- [ ] **Step 4: Align default table name**

Set `JfoundryOutboxProperties.tableName` default to:

```java
private String tableName = "jfoundry_outbox_event";
```

Ensure `OutboxData` has:

```java
@TableName("jfoundry_outbox_event")
```

Update SQL scripts and docs from `ddd_outbox_event` to `jfoundry_outbox_event`, except where documenting migration from the old name.

- [ ] **Step 5: Run table-name tests**

Run:

```bash
mvn -pl jfoundry-spring/jfoundry-autoconfigure -am -Dtest='OutboxTableNameOverrideTest,OutboxTableNameWithCustomInterceptorTest' -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit**

```bash
git add jfoundry-spring/jfoundry-autoconfigure jfoundry-infrastructure/jfoundry-outbox-mybatis-plus README.md docs
git commit -m "fix(outbox): make table name override reliable"
```

---

## Task 3: Add Broker-Neutral Aggregate Routing Metadata

**Files:**
- Create: `AggregateRouting.java`
- Create: `AggregateRoutingMetadata.java`
- Create: `AggregateRoutingResolver.java`
- Modify: `ExternalizationRuleResolver.java` if property-path reading is extracted or reused.
- Modify: `OutboxEntry.java`
- Modify: `OutboxData.java`
- Modify: outbox SQL scripts.
- Modify: `DomainEventExternalizer.java`
- Add tests in messaging-core, outbox-core, outbox-mybatis-plus, and outbox-spring/autoconfigure as needed.

- [ ] **Step 1: Write failing resolver tests**

Create:

```text
jfoundry-infrastructure/jfoundry-messaging-core/src/test/java/org/jfoundry/infrastructure/messaging/externalization/AggregateRoutingResolverTest.java
```

Add tests:

```java
@AggregateRouting(type = "Order", id = "orderId", version = "version")
static class OrderEvent implements DomainEvent {
    String getOrderId() { return "order-1"; }
    long getVersion() { return 7L; }
}

@Test
void resolvesAggregateRoutingMetadata() {
    AggregateRoutingResolver resolver = new AggregateRoutingResolver();
    AggregateRoutingMetadata metadata = resolver.resolve(new OrderEvent()).orElseThrow();

    assertThat(metadata.aggregateType()).isEqualTo("Order");
    assertThat(metadata.aggregateId()).isEqualTo("order-1");
    assertThat(metadata.aggregateVersion()).isEqualTo(7L);
}
```

Run:

```bash
mvn -pl jfoundry-infrastructure/jfoundry-messaging-core -am -Dtest=AggregateRoutingResolverTest test
```

Expected: FAIL because classes do not exist.

- [ ] **Step 2: Implement aggregate routing metadata**

Add:

```java
public record AggregateRoutingMetadata(String aggregateType, String aggregateId, Long aggregateVersion) {
}
```

Add `AggregateRouting` annotation:

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AggregateRouting {
    String type() default "";
    String id();
    String version() default "";
}
```

Add `AggregateRoutingResolver` with simple property path reading compatible with `ExternalizationRuleResolver`.

- [ ] **Step 3: Extend OutboxEntry and tests**

Add fields:

```java
private String aggregateType;
private String aggregateId;
private Long aggregateVersion;
```

Add getters/setters.

Add overloaded factory:

```java
public static OutboxEntry newPending(
        String eventId, String topic, String payloadKey,
        String payloadType, String payloadJson, Instant occurredAt,
        String aggregateType, String aggregateId, Long aggregateVersion) {
    OutboxEntry entry = newPending(eventId, topic, payloadKey, payloadType, payloadJson, occurredAt);
    entry.aggregateType = aggregateType;
    entry.aggregateId = aggregateId;
    entry.aggregateVersion = aggregateVersion;
    return entry;
}
```

Update `OutboxEntryTest` to assert aggregate fields are retained.

- [ ] **Step 4: Extend persistence mapping and SQL**

Add columns to all outbox create-table scripts:

```sql
aggregate_type    VARCHAR(255),
aggregate_id      VARCHAR(255),
aggregate_version BIGINT,
```

Add index:

```sql
CREATE INDEX idx_outbox_aggregate ON jfoundry_outbox_event (aggregate_type, aggregate_id, aggregate_version);
```

Update `OutboxData` fields and `fromEntry` / `toEntry`.

Add/extend persistence test to append and read aggregate metadata.

- [ ] **Step 5: Update DomainEventExternalizer**

Inject or construct `AggregateRoutingResolver`.

When handling an event:

- resolve `ExternalizationRule`;
- resolve `AggregateRoutingMetadata`;
- if `rule.payloadKey()` is null and metadata has aggregate id, use aggregate id as payload key;
- create `OutboxEntry` with aggregate fields.

Add test that an event annotated with `@Externalized` and `@AggregateRouting` lands in outbox with aggregate fields and payload key fallback.

- [ ] **Step 6: Run aggregate metadata tests**

Run:

```bash
mvn -pl jfoundry-infrastructure/jfoundry-messaging-core,jfoundry-infrastructure/jfoundry-outbox-core,jfoundry-infrastructure/jfoundry-outbox-mybatis-plus,jfoundry-spring/jfoundry-autoconfigure -am test
```

Expected: BUILD SUCCESS.

- [ ] **Step 7: Commit**

```bash
git add jfoundry-infrastructure/jfoundry-messaging-core jfoundry-infrastructure/jfoundry-outbox-core jfoundry-infrastructure/jfoundry-outbox-mybatis-plus jfoundry-infrastructure/jfoundry-outbox-spring jfoundry-spring/jfoundry-autoconfigure
git commit -m "feat(outbox): add aggregate routing metadata"
```

---

## Task 4: Add Kafka MessageSender Adapter

**Files:**
- Create module `jfoundry-infrastructure/jfoundry-messaging-kafka`
- Modify `jfoundry-infrastructure/pom.xml`
- Modify `jfoundry-dependencies/pom.xml`
- Modify `jfoundry-spring/jfoundry-autoconfigure/pom.xml`
- Modify `jfoundry-spring/jfoundry-spring-boot-starter/pom.xml` if starter should include Kafka adapter by default.
- Create Kafka auto-config classes and tests.

- [ ] **Step 1: Add module skeleton and dependency management**

Add module to `jfoundry-infrastructure/pom.xml`:

```xml
<module>jfoundry-messaging-kafka</module>
```

Add managed dependency for `jfoundry-messaging-kafka` and Spring Kafka:

```xml
<dependency>
    <groupId>org.jfoundry</groupId>
    <artifactId>jfoundry-messaging-kafka</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
    <version>${spring-kafka.version}</version>
</dependency>
```

Use the Spring Kafka version aligned with Spring Boot 3.2.7 dependency management if the BOM already supplies it; otherwise add an explicit property.

- [ ] **Step 2: Write failing Kafka sender tests**

Create `KafkaMessageSenderTest` with Mockito.

Test success:

```java
when(kafkaTemplate.send("order.created", "order-1", "{}"))
        .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

SendResult result = sender.send("order.created", "order-1", "{}");

assertThat(result.success()).isTrue();
```

Test failure:

```java
CompletableFuture<org.springframework.kafka.support.SendResult<String, String>> failed = new CompletableFuture<>();
failed.completeExceptionally(new IllegalStateException("broker down"));
when(kafkaTemplate.send("order.created", "order-1", "{}")).thenReturn(failed);

SendResult result = sender.send("order.created", "order-1", "{}");

assertThat(result.success()).isFalse();
assertThat(result.errorMessage()).contains("broker down");
```

Run:

```bash
mvn -pl jfoundry-infrastructure/jfoundry-messaging-kafka -am test
```

Expected: FAIL because implementation does not exist.

- [ ] **Step 3: Implement KafkaMessageSender**

Implement a synchronous sender with timeout:

```java
public class KafkaMessageSender implements MessageSender {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Duration sendTimeout;

    public KafkaMessageSender(KafkaTemplate<String, String> kafkaTemplate, Duration sendTimeout) {
        this.kafkaTemplate = kafkaTemplate;
        this.sendTimeout = sendTimeout;
    }

    @Override
    public org.jfoundry.infrastructure.messaging.SendResult send(String topic, String payloadKey, String payload) {
        try {
            kafkaTemplate.send(topic, payloadKey, payload)
                    .get(sendTimeout.toMillis(), TimeUnit.MILLISECONDS);
            return org.jfoundry.infrastructure.messaging.SendResult.ok();
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            return org.jfoundry.infrastructure.messaging.SendResult.fail(cause.getMessage());
        }
    }
}
```

- [ ] **Step 4: Add Kafka auto-configuration tests**

Create `KafkaMessageSenderAutoConfigurationTest` using `ApplicationContextRunner`.

Test cases:

- creates `KafkaMessageSender` when `KafkaTemplate` bean exists and no `MessageSender` exists;
- backs off when user provides `MessageSender`;
- disabled when `jfoundry.messaging.kafka.enabled=false`.

- [ ] **Step 5: Implement Kafka auto-configuration**

Create:

```java
@ConfigurationProperties(prefix = "jfoundry.messaging.kafka")
public class KafkaMessageSenderProperties {
    private boolean enabled = true;
    private Duration sendTimeout = Duration.ofSeconds(10);
}
```

Create:

```java
@AutoConfiguration
@ConditionalOnClass(KafkaTemplate.class)
@EnableConfigurationProperties(KafkaMessageSenderProperties.class)
@ConditionalOnProperty(prefix = "jfoundry.messaging.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KafkaMessageSenderAutoConfiguration {
    @Bean
    @ConditionalOnBean(KafkaTemplate.class)
    @ConditionalOnMissingBean(MessageSender.class)
    public KafkaMessageSender kafkaMessageSender(
            KafkaTemplate<String, String> kafkaTemplate,
            KafkaMessageSenderProperties properties) {
        return new KafkaMessageSender(kafkaTemplate, properties.getSendTimeout());
    }
}
```

Add it to `AutoConfiguration.imports`.

- [ ] **Step 6: Run Kafka tests**

Run:

```bash
mvn -pl jfoundry-infrastructure/jfoundry-messaging-kafka,jfoundry-spring/jfoundry-autoconfigure -am -Dtest='KafkaMessageSenderTest,KafkaMessageSenderAutoConfigurationTest' -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: BUILD SUCCESS.

- [ ] **Step 7: Commit**

```bash
git add jfoundry-infrastructure/pom.xml jfoundry-dependencies/pom.xml jfoundry-infrastructure/jfoundry-messaging-kafka jfoundry-spring/jfoundry-autoconfigure jfoundry-spring/jfoundry-spring-boot-starter/pom.xml
git commit -m "feat(messaging): add kafka message sender"
```

---

## Task 5: Add Inbox Core

**Files:**
- Create module `jfoundry-infrastructure/jfoundry-inbox-core`
- Modify `jfoundry-infrastructure/pom.xml`
- Modify `jfoundry-dependencies/pom.xml`

- [ ] **Step 1: Write failing InboxTemplate tests**

Create `InboxTemplateTest`.

Test already processed skip:

```java
RecordingInboxRepository repository = new RecordingInboxRepository();
repository.processed = true;
InboxTemplate template = new InboxTemplate(repository);
AtomicBoolean called = new AtomicBoolean(false);

boolean executed = template.executeOnce("evt-1", "projection", () -> called.set(true));

assertThat(executed).isFalse();
assertThat(called).isFalse();
```

Test success records processed:

```java
RecordingInboxRepository repository = new RecordingInboxRepository();
InboxTemplate template = new InboxTemplate(repository);

boolean executed = template.executeOnce("evt-1", "projection", () -> {});

assertThat(executed).isTrue();
assertThat(repository.recordedMessageId).isEqualTo("evt-1");
assertThat(repository.recordedConsumerName).isEqualTo("projection");
```

Test failure does not record:

```java
RecordingInboxRepository repository = new RecordingInboxRepository();
InboxTemplate template = new InboxTemplate(repository);

assertThatThrownBy(() -> template.executeOnce("evt-1", "projection", () -> {
    throw new IllegalStateException("boom");
})).isInstanceOf(IllegalStateException.class);

assertThat(repository.recordedMessageId).isNull();
```

Run:

```bash
mvn -pl jfoundry-infrastructure/jfoundry-inbox-core -am test
```

Expected: FAIL because module/classes do not exist.

- [ ] **Step 2: Implement inbox core types**

Create:

```java
@FunctionalInterface
public interface InboxHandler {
    void handle();
}
```

Create:

```java
public enum InboxMessageStatus {
    PROCESSED
}
```

Create:

```java
public class InboxMessage {
    private String messageId;
    private String consumerName;
    private InboxMessageStatus status;
    private Instant processedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private String errorMessage;

    public static InboxMessage processed(String messageId, String consumerName) { ... }
}
```

Create:

```java
public interface InboxRepository {
    boolean isProcessed(String messageId, String consumerName);
    void markProcessed(String messageId, String consumerName);
}
```

Create:

```java
public class InboxTemplate {
    private final InboxRepository repository;

    public boolean executeOnce(String messageId, String consumerName, InboxHandler handler) {
        if (repository.isProcessed(messageId, consumerName)) {
            return false;
        }
        handler.handle();
        repository.markProcessed(messageId, consumerName);
        return true;
    }
}
```

Validate `messageId`, `consumerName`, and `handler` are not null/blank.

- [ ] **Step 3: Run inbox core tests**

Run:

```bash
mvn -pl jfoundry-infrastructure/jfoundry-inbox-core -am test
```

Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add jfoundry-infrastructure/pom.xml jfoundry-dependencies/pom.xml jfoundry-infrastructure/jfoundry-inbox-core
git commit -m "feat(inbox): add idempotent execution core"
```

---

## Task 6: Add Inbox MyBatis-Plus Persistence

**Files:**
- Create module `jfoundry-infrastructure/jfoundry-inbox-mybatis-plus`
- Modify `jfoundry-infrastructure/pom.xml`
- Modify `jfoundry-dependencies/pom.xml`
- Create inbox persistence classes and SQL.

- [ ] **Step 1: Write failing MyBatis inbox repository tests**

Create `MybatisPlusInboxRepositoryTest`.

Tests:

- `markProcessedPersistsProcessedMessage`
- `isProcessedReturnsTrueForExistingProcessedMessage`
- `duplicateMarkProcessedIsIdempotent`
- `differentConsumersCanProcessSameMessage`

Run:

```bash
mvn -pl jfoundry-infrastructure/jfoundry-inbox-mybatis-plus -am test
```

Expected: FAIL because module/classes do not exist.

- [ ] **Step 2: Implement inbox data and mapper**

Create `InboxMessageData`:

```java
@TableName("jfoundry_inbox_message")
public class InboxMessageData {
    private String messageId;
    private String consumerName;
    private String status;
    private Instant processedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private String errorMessage;
}
```

Because the primary key is composite, use wrapper-based queries instead of relying on `selectById`.

Create `InboxMessageMapper extends BaseMapper<InboxMessageData>`.

- [ ] **Step 3: Implement MybatisPlusInboxRepository**

Implement:

```java
public class MybatisPlusInboxRepository implements InboxRepository {
    private final InboxMessageMapper mapper;

    public boolean isProcessed(String messageId, String consumerName) {
        Long count = mapper.selectCount(Wrappers.lambdaQuery(InboxMessageData.class)
                .eq(InboxMessageData::getMessageId, messageId)
                .eq(InboxMessageData::getConsumerName, consumerName)
                .eq(InboxMessageData::getStatus, InboxMessageStatus.PROCESSED.name()));
        return count != null && count > 0;
    }

    public void markProcessed(String messageId, String consumerName) {
        InboxMessageData data = InboxMessageData.processed(messageId, consumerName);
        try {
            mapper.insert(data);
        } catch (DuplicateKeyException ignored) {
            // already processed by a racing consumer
        }
    }
}
```

Use the MyBatis/Spring exception type actually thrown in tests; if MyBatis wraps duplicate key differently, catch Spring's `DuplicateKeyException` at the repository boundary.

- [ ] **Step 4: Add SQL**

Create `inbox_message.sql` and migration:

```sql
CREATE TABLE jfoundry_inbox_message (
    message_id    VARCHAR(128)  NOT NULL,
    consumer_name VARCHAR(255)  NOT NULL,
    status        VARCHAR(32)   NOT NULL,
    processed_at  TIMESTAMP,
    created_at    TIMESTAMP     NOT NULL,
    updated_at    TIMESTAMP     NOT NULL,
    error_message VARCHAR(2000),
    PRIMARY KEY (consumer_name, message_id)
);
CREATE INDEX idx_inbox_processed_at ON jfoundry_inbox_message (processed_at);
```

- [ ] **Step 5: Run inbox persistence tests**

Run:

```bash
mvn -pl jfoundry-infrastructure/jfoundry-inbox-mybatis-plus -am test
```

Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit**

```bash
git add jfoundry-infrastructure/pom.xml jfoundry-dependencies/pom.xml jfoundry-infrastructure/jfoundry-inbox-mybatis-plus
git commit -m "feat(inbox): add mybatis-plus persistence"
```

---

## Task 7: Add Inbox Auto-Configuration

**Files:**
- Create: `InboxAutoConfiguration.java`
- Create: `InboxMybatisPlusAutoConfiguration.java`
- Modify: `AutoConfiguration.imports`
- Modify: `jfoundry-spring/jfoundry-autoconfigure/pom.xml`
- Add tests.

- [ ] **Step 1: Write failing auto-config tests**

Create `InboxAutoConfigurationTest`.

Test:

- creates `InboxTemplate` when `InboxRepository` exists;
- backs off when user provides `InboxTemplate`;
- creates `MybatisPlusInboxRepository` when mapper and MyBatis-Plus are present.

Run:

```bash
mvn -pl jfoundry-spring/jfoundry-autoconfigure -am -Dtest=InboxAutoConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: FAIL because auto-config classes do not exist.

- [ ] **Step 2: Implement auto-config**

Create:

```java
@AutoConfiguration
@ConditionalOnClass(InboxTemplate.class)
public class InboxAutoConfiguration {
    @Bean
    @ConditionalOnBean(InboxRepository.class)
    @ConditionalOnMissingBean(InboxTemplate.class)
    public InboxTemplate inboxTemplate(InboxRepository repository) {
        return new InboxTemplate(repository);
    }
}
```

Create:

```java
@AutoConfiguration
@MapperScan(basePackages = "org.jfoundry.infrastructure.inbox.mybatis")
@ConditionalOnClass(MybatisPlusInboxRepository.class)
public class InboxMybatisPlusAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(InboxRepository.class)
    public InboxRepository inboxRepository(InboxMessageMapper mapper) {
        return new MybatisPlusInboxRepository(mapper);
    }
}
```

Add both to `AutoConfiguration.imports`.

- [ ] **Step 3: Run auto-config tests**

Run:

```bash
mvn -pl jfoundry-spring/jfoundry-autoconfigure -am -Dtest=InboxAutoConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add jfoundry-spring/jfoundry-autoconfigure
git commit -m "feat(inbox): add spring boot auto-configuration"
```

---

## Task 8: Update Starter, BOM, and Documentation

**Files:**
- Modify: `jfoundry-dependencies/pom.xml`
- Modify: `jfoundry-spring/jfoundry-spring-boot-starter/pom.xml`
- Modify: `README.md`
- Modify: `docs/transactional-outbox.md`
- Modify: `docs/framework-boundaries.md`

- [ ] **Step 1: Update dependency surfaces**

Ensure the BOM contains:

```xml
<dependency>
    <groupId>org.jfoundry</groupId>
    <artifactId>jfoundry-messaging-kafka</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>org.jfoundry</groupId>
    <artifactId>jfoundry-inbox-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>org.jfoundry</groupId>
    <artifactId>jfoundry-inbox-mybatis-plus</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

Starter policy:

- include `jfoundry-inbox-core` and `jfoundry-inbox-mybatis-plus` because they are default reliability infrastructure;
- include `jfoundry-messaging-kafka` only if the project wants Kafka to be part of the default starter. If avoiding an implicit Kafka dependency is preferred, document it as optional. The recommended choice for this implementation is optional, because not every outbox user uses Kafka.

- [ ] **Step 2: Update README**

Add module list entries:

```text
jfoundry-messaging-kafka                 Kafka MessageSender adapter
jfoundry-inbox-core                      Consumer-side idempotency SPI/template
jfoundry-inbox-mybatis-plus              Inbox MyBatis-Plus persistence adapter
```

Clarify:

- default outbox table is `jfoundry_outbox_event`;
- `jfoundry.outbox.table-name` overrides the physical table name;
- Kafka sender is the first real broker adapter;
- InboxTemplate is explicit consumer-side idempotency support.

- [ ] **Step 3: Update transactional outbox docs**

Document:

```text
jfoundry implements the polling publisher variant of Transactional Outbox.
transaction-log tailing / Debezium is not part of the default runtime.
Kafka sender maps MessageSender topic/key/payload to Kafka topic/key/value.
Aggregate metadata is broker-neutral.
InboxTemplate.executeOnce(...) provides consumer idempotency.
```

Add code sample:

```java
inboxTemplate.executeOnce(eventId, "order-projection", () -> {
    handler.handle(event);
});
```

- [ ] **Step 4: Run docs-adjacent validation**

Run:

```bash
mvn validate
```

Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add jfoundry-dependencies/pom.xml jfoundry-spring/jfoundry-spring-boot-starter/pom.xml README.md docs
git commit -m "docs(outbox): document kafka and inbox reliability"
```

---

## Task 9: Full Verification

**Files:** no expected source changes unless fixing test failures.

- [ ] **Step 1: Search for stale names and packages**

Run:

```bash
rg -n "ddd_outbox_event|org\\.jfoundry\\.autoconfigure\\.dispatcher|org\\.jfoundry\\.autoconfigure\\.persistence\\.JfoundryOutboxProperties|org\\.jfoundry\\.autoconfigure\\.persistence\\.OutboxMybatisPlusAutoConfiguration" .
```

Expected:

- no stale package references;
- `ddd_outbox_event` appears only in compatibility/migration documentation if retained.

- [ ] **Step 2: Run full test suite**

Run:

```bash
mvn test
```

Expected: BUILD SUCCESS.

- [ ] **Step 3: Inspect final diff**

Run:

```bash
git status --short
git log --oneline -10
```

Expected:

- working tree clean after all task commits;
- recent commits correspond to the task commits above.
