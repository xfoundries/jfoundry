# Integration Verification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a middleware-backed verification matrix for Inbox idempotency, Outbox claim-token safety, and Kafka dispatch behavior.

**Architecture:** Add a dedicated `jfoundry-integration-tests` Maven module that is inert during `mvn test` and active only during `mvn verify -Pit`. MySQL and Kafka run through Testcontainers; Dameng runs through an external `dm-it` profile using environment-provided JDBC settings and reflective driver loading.

**Tech Stack:** Java 21, Maven, JUnit Jupiter, AssertJ, Spring Boot Test/JDBC, MyBatis-Plus, Testcontainers `1.21.4`, MySQL Connector/J `9.7.0`, Spring Kafka `3.1.6`.

---

## File Structure

- Modify `pom.xml`: add Failsafe version, add `jfoundry-integration-tests` to the reactor, and add plugin management for Failsafe.
- Modify `jfoundry-dependencies/pom.xml`: manage Testcontainers, MySQL Connector/J, Spring Kafka Test, and Kafka clients where needed.
- Create `jfoundry-integration-tests/pom.xml`: isolated module with Failsafe enabled under `it`; default build skips `*IT`.
- Create `jfoundry-integration-tests/src/test/java/org/jfoundry/integration/support/OutboxInboxDatabaseConfig.java`: MyBatis-Plus mapper scanning and store beans for SQL-backed tests.
- Create `jfoundry-integration-tests/src/test/java/org/jfoundry/integration/support/SqlScripts.java`: executes production migration scripts against a container or external database.
- Create `jfoundry-integration-tests/src/test/java/org/jfoundry/integration/support/OutboxMessages.java`: reusable Outbox test message factory.
- Create `jfoundry-integration-tests/src/test/java/org/jfoundry/integration/mysql/MySqlOutboxStoreIT.java`: MySQL Outbox claim-token/recovery/cleanup/large-payload matrix.
- Create `jfoundry-integration-tests/src/test/java/org/jfoundry/integration/mysql/MySqlInboxStoreIT.java`: MySQL Inbox concurrent idempotency matrix.
- Create `jfoundry-integration-tests/src/test/java/org/jfoundry/integration/kafka/KafkaOutboxDispatchIT.java`: Kafka broker plus Outbox dispatcher success/failure verification.
- Create `jfoundry-integration-tests/src/test/java/org/jfoundry/integration/dm/DamengStoreIT.java`: external DM8 store-level verification, skipped only when explicit DM configuration is missing.
- Create `docs/outbox/integration-verification.md`: operator-facing commands and environment notes.

## Task 1: Maven Wiring

**Files:**
- Modify: `pom.xml`
- Modify: `jfoundry-dependencies/pom.xml`
- Create: `jfoundry-integration-tests/pom.xml`

- [ ] **Step 1: Add failing reactor check**

Run:

```bash
mvn -pl jfoundry-integration-tests test
```

Expected: FAIL because the module does not exist.

- [ ] **Step 2: Add parent plugin/dependency management**

In `pom.xml`, add:

```xml
<maven-failsafe-plugin.version>3.5.2</maven-failsafe-plugin.version>
```

Add the module after `jfoundry-spring`:

```xml
<module>jfoundry-integration-tests</module>
```

Add Failsafe to `<pluginManagement>`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-failsafe-plugin</artifactId>
    <version>${maven-failsafe-plugin.version}</version>
</plugin>
```

In `jfoundry-dependencies/pom.xml`, add properties:

```xml
<testcontainers.version>1.21.4</testcontainers.version>
<mysql-connector-j.version>9.7.0</mysql-connector-j.version>
```

Add managed dependencies:

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>${testcontainers.version}</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mysql</artifactId>
    <version>${testcontainers.version}</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>kafka</artifactId>
    <version>${testcontainers.version}</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <version>${mysql-connector-j.version}</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka-test</artifactId>
    <version>${spring-kafka.version}</version>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 3: Create integration module POM**

Create `jfoundry-integration-tests/pom.xml` with packaging `jar`, parent `jfoundry-parent`, and dependencies on:

```xml
org.jfoundry:jfoundry-outbox-core
org.jfoundry:jfoundry-inbox-core
org.jfoundry:jfoundry-messaging-core
org.jfoundry:jfoundry-outbox-mybatis-plus
org.jfoundry:jfoundry-inbox-mybatis-plus
org.jfoundry:jfoundry-messaging-kafka
org.springframework.boot:spring-boot-starter-test
org.springframework.boot:spring-boot-starter-jdbc
com.baomidou:mybatis-plus-spring-boot3-starter
com.baomidou:mybatis-plus-jsqlparser
org.junit.jupiter:junit-jupiter
org.assertj:assertj-core
org.awaitility:awaitility
org.testcontainers:junit-jupiter
org.testcontainers:mysql
org.testcontainers:kafka
com.mysql:mysql-connector-j
org.springframework.kafka:spring-kafka
org.springframework.kafka:spring-kafka-test
```

Configure Surefire to exclude integration tests:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <excludes>
            <exclude>**/*IT.java</exclude>
        </excludes>
    </configuration>
</plugin>
```

Configure Failsafe only under profile `it`:

```xml
<profile>
    <id>it</id>
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-failsafe-plugin</artifactId>
                <configuration>
                    <includes>
                        <include>**/*IT.java</include>
                    </includes>
                    <excludes>
                        <exclude>**/dm/*IT.java</exclude>
                    </excludes>
                </configuration>
                <executions>
                    <execution>
                        <goals>
                            <goal>integration-test</goal>
                            <goal>verify</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</profile>
```

Add profile `dm-it` that overrides Failsafe excludes to include `**/dm/*IT.java`.

- [ ] **Step 4: Verify default build remains inert**

Run:

```bash
mvn -pl jfoundry-integration-tests test
```

Expected: PASS, no Docker containers start, no `*IT` classes execute.

- [ ] **Step 5: Commit**

```bash
git add pom.xml jfoundry-dependencies/pom.xml jfoundry-integration-tests/pom.xml
git commit -m "test(integration): add verification module wiring"
```

## Task 2: Shared Integration Test Support

**Files:**
- Create: `jfoundry-integration-tests/src/test/java/org/jfoundry/integration/support/OutboxInboxDatabaseConfig.java`
- Create: `jfoundry-integration-tests/src/test/java/org/jfoundry/integration/support/SqlScripts.java`
- Create: `jfoundry-integration-tests/src/test/java/org/jfoundry/integration/support/OutboxMessages.java`

- [ ] **Step 1: Write minimal support classes**

`OutboxInboxDatabaseConfig` must:

- Use `@SpringBootConfiguration`, `@EnableAutoConfiguration`, and `@MapperScan` for `OutboxMapper` and `InboxMessageMapper`.
- Provide a `MybatisPlusInterceptor` bean with `new PaginationInnerInterceptor(DbType.MYSQL)`.
- Provide `MybatisPlusOutboxMessageStore` and `MybatisPlusInboxMessageStore` beans.

`SqlScripts` must expose:

```java
public static void run(DataSource dataSource, String... classpathScripts)
```

It should use `ResourceDatabasePopulator` and `ClassPathResource`.

`OutboxMessages` must expose:

```java
public static OutboxMessage pending(String eventId)
public static OutboxMessage pending(String eventId, String topic, String key, String payload)
public static String payloadOfSize(int bytes)
```

- [ ] **Step 2: Compile support code**

Run:

```bash
mvn -pl jfoundry-integration-tests -Pit test
```

Expected: PASS, no IT classes yet.

- [ ] **Step 3: Commit**

```bash
git add jfoundry-integration-tests/src/test/java/org/jfoundry/integration/support
git commit -m "test(integration): add shared middleware test support"
```

## Task 3: MySQL Outbox Verification

**Files:**
- Create: `jfoundry-integration-tests/src/test/java/org/jfoundry/integration/mysql/MySqlOutboxStoreIT.java`

- [ ] **Step 1: Write failing tests**

Create tests named:

- `claimDispatchableClaimsEachMessageOnceUnderConcurrency`
- `markAsPublishedRequiresCurrentClaimToken`
- `markAsFailedRequiresCurrentClaimToken`
- `recoverStuckDispatchingReleasesExpiredClaims`
- `deleteByStatusAndOccurredAtBeforeKeepsRecentAndActiveMessages`
- `largePayloadRoundTripsThroughMysql`

Use `@Testcontainers`, a static `MySQLContainer<?>`, `@DynamicPropertySource`, and `@SpringBootTest(classes = OutboxInboxDatabaseConfig.class)`.

Use the production migration script:

```java
SqlScripts.run(dataSource,
        "db/migration/V20260617__create_outbox_event.sql");
```

The stale-token tests must follow this pattern:

```java
store.append(OutboxMessages.pending("evt-1"));
OutboxMessage firstClaim = store.claimDispatchable(1, "pod-a").get(0);
store.recoverStuckDispatching(Instant.now().plusSeconds(1));
OutboxMessage secondClaim = store.claimDispatchable(1, "pod-b").get(0);
store.markAsPublished("evt-1", firstClaim.getClaimToken());
OutboxData data = mapper.selectById("evt-1");
assertThat(data.getStatus()).isEqualTo("DISPATCHING");
assertThat(data.getClaimToken()).isEqualTo(secondClaim.getClaimToken());
```

- [ ] **Step 2: Run tests to verify failures before implementation fixes**

Run:

```bash
mvn -pl jfoundry-integration-tests -Pit -DskipITs=false -Dtest=none -Dit.test=MySqlOutboxStoreIT verify
```

Expected: FAIL only for missing test support details or schema reset issues. If it passes immediately after writing tests, keep the tests and continue because the production code already contains the intended hardening.

- [ ] **Step 3: Finish implementation**

Ensure each test cleans the table with:

```java
mapper.delete(null);
```

For concurrency, use `ExecutorService`, `CountDownLatch`, and collect claimed event ids. Assert:

```java
assertThat(claimedIds).containsExactlyInAnyOrder("evt-1", "evt-2");
assertThat(claimedIds).doesNotHaveDuplicates();
```

For large payload, use at least `1024 * 1024 + 128` bytes and assert exact equality after load.

- [ ] **Step 4: Verify MySQL Outbox IT**

Run:

```bash
mvn -pl jfoundry-integration-tests -Pit -DskipITs=false -Dtest=none -Dit.test=MySqlOutboxStoreIT verify
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add jfoundry-integration-tests/src/test/java/org/jfoundry/integration/mysql/MySqlOutboxStoreIT.java
git commit -m "test(outbox): verify mysql claim-token safety"
```

## Task 4: MySQL Inbox Verification

**Files:**
- Create: `jfoundry-integration-tests/src/test/java/org/jfoundry/integration/mysql/MySqlInboxStoreIT.java`

- [ ] **Step 1: Write failing tests**

Create tests named:

- `tryStartProcessingAllowsOnlyOneConcurrentHandler`
- `markProcessedTransitionsProcessingRecord`
- `markFailedAllowsRetry`
- `processedMessageCannotBeStartedAgain`
- `differentConsumersCanProcessSameMessage`

Use the production migration script:

```java
SqlScripts.run(dataSource,
        "db/migration/V20260624__create_inbox_message.sql");
```

The concurrency test must call `store.tryStartProcessing("evt-1", "projection")` from at least four threads and assert exactly one `true`.

- [ ] **Step 2: Run tests**

Run:

```bash
mvn -pl jfoundry-integration-tests -Pit -DskipITs=false -Dtest=none -Dit.test=MySqlInboxStoreIT verify
```

Expected: PASS after the current Inbox hardening; failures indicate a real MySQL unique-key or transaction behavior gap.

- [ ] **Step 3: Commit**

```bash
git add jfoundry-integration-tests/src/test/java/org/jfoundry/integration/mysql/MySqlInboxStoreIT.java
git commit -m "test(inbox): verify mysql idempotent processing"
```

## Task 5: Kafka Outbox Dispatch Verification

**Files:**
- Create: `jfoundry-integration-tests/src/test/java/org/jfoundry/integration/kafka/KafkaOutboxDispatchIT.java`

- [ ] **Step 1: Write Kafka success-path test**

Use:

- `@Testcontainers`
- `KafkaContainer`
- `MySQLContainer<?>`
- `KafkaTemplate<String, String>`
- `DefaultKafkaConsumerFactory`
- `DefaultOutboxDispatchService`
- `KafkaMessageSender`

Test name:

```java
dispatchPublishesKafkaRecordAndMarksOutboxPublished()
```

Arrange:

```java
store.append(OutboxMessages.pending(
        "evt-kafka-1",
        "jfoundry.integration.outbox",
        "order-1",
        "{\"event\":\"created\"}"));
```

Act:

```java
new DefaultOutboxDispatchService(
        store,
        new KafkaMessageSender(kafkaTemplate, Duration.ofSeconds(10)),
        3,
        retry -> Duration.ofMillis(10),
        "it-pod").dispatch(10);
```

Assert:

- Kafka receives the expected topic/key/value.
- `mapper.selectById("evt-kafka-1").getStatus()` is `PUBLISHED`.
- `claim_token`, `claimed_at`, and `claimed_by` are null after completion.

- [ ] **Step 2: Write failure-path test**

Test name:

```java
dispatchFailureMarksOutboxFailedAndKeepsRetryMetadata()
```

Use a `MessageSender` lambda returning:

```java
SendResult.fail("broker down")
```

Assert:

- status is `FAILED`
- `retry_count` is `1`
- `error_message` contains `broker down`
- `next_retry_at` is not null
- claim metadata is cleared

- [ ] **Step 3: Verify Kafka IT**

Run:

```bash
mvn -pl jfoundry-integration-tests -Pit -DskipITs=false -Dtest=none -Dit.test=KafkaOutboxDispatchIT verify
```

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add jfoundry-integration-tests/src/test/java/org/jfoundry/integration/kafka/KafkaOutboxDispatchIT.java
git commit -m "test(kafka): verify outbox dispatch against broker"
```

## Task 6: Dameng External Verification

**Files:**
- Create: `jfoundry-integration-tests/src/test/java/org/jfoundry/integration/dm/DamengStoreIT.java`
- Modify: `jfoundry-integration-tests/pom.xml`

- [ ] **Step 1: Add DM profile include behavior**

In profile `dm-it`, configure Failsafe to include only:

```xml
<include>**/dm/*IT.java</include>
```

This keeps DM out of the normal `-Pit` MySQL/Kafka job.

- [ ] **Step 2: Write DM test**

`DamengStoreIT` must:

- Read `DM_JDBC_URL`, `DM_USERNAME`, `DM_PASSWORD`, and optional `DM_DRIVER_CLASS`.
- Use `Assumptions.assumeTrue` to skip only when one of the three required connection variables is missing.
- Default `DM_DRIVER_CLASS` to `dm.jdbc.driver.DmDriver`.
- Call `Class.forName(driverClass)` and fail with a clear assertion if the driver is missing while connection variables are present.
- Build a `DriverManagerDataSource`.
- Run `db/migration/V20260617__create_outbox_event_dm.sql` and `db/migration/V20260624__create_inbox_message.sql`.
- Reuse the same store assertions as MySQL for stale-token rejection and Inbox duplicate-start rejection.

- [ ] **Step 3: Verify DM skip behavior**

Run without DM env vars:

```bash
mvn -pl jfoundry-integration-tests -Pit,dm-it -DskipITs=false -Dtest=none -Dit.test=DamengStoreIT verify
```

Expected: PASS with skipped DM test and a clear skip reason.

- [ ] **Step 4: Document real DM execution**

Add the command:

```bash
DM_JDBC_URL='jdbc:dm://host:5236/SCHEMA' \
DM_USERNAME='SYSDBA' \
DM_PASSWORD='password' \
DM_DRIVER_CLASS='dm.jdbc.driver.DmDriver' \
mvn -pl jfoundry-integration-tests -Pit,dm-it -DskipITs=false -Dtest=none -Dit.test=DamengStoreIT verify
```

Note that the DM JDBC driver must be available on the test runtime classpath for this command to connect.

- [ ] **Step 5: Commit**

```bash
git add jfoundry-integration-tests/pom.xml jfoundry-integration-tests/src/test/java/org/jfoundry/integration/dm/DamengStoreIT.java
git commit -m "test(dm): add external store verification profile"
```

## Task 7: Documentation And Full Verification

**Files:**
- Create: `docs/outbox/integration-verification.md`

- [ ] **Step 1: Write operator documentation**

Document:

- `mvn test` for default unit verification.
- `mvn verify -Pit` for MySQL/Kafka Testcontainers verification.
- `mvn verify -Pit,dm-it` for external DM verification.
- Docker requirement for Testcontainers.
- DM environment variables and driver classpath requirement.
- CI job split: `unit`, `integration-mysql-kafka`, `integration-dm`.

- [ ] **Step 2: Run default verification**

Run:

```bash
mvn test
```

Expected: PASS and no middleware containers required.

- [ ] **Step 3: Run MySQL/Kafka verification**

Run:

```bash
mvn -pl jfoundry-integration-tests -Pit verify
```

Expected: PASS when Docker is available.

- [ ] **Step 4: Run DM skip verification**

Run:

```bash
mvn -pl jfoundry-integration-tests -Pit,dm-it -Dtest=none -Dit.test=DamengStoreIT verify
```

Expected: PASS with skipped DM test when DM env vars are absent.

- [ ] **Step 5: Commit**

```bash
git add docs/outbox/integration-verification.md
git commit -m "docs: document integration verification matrix"
```

## Final Checks

- [ ] Run `git status --short` and confirm only expected untracked local artifacts remain.
- [ ] Summarize which commands passed.
- [ ] If Docker is unavailable, report that `mvn -pl jfoundry-integration-tests -Pit verify` could not be completed and include the exact failure reason.
- [ ] Do not add `graphify-out/` to any commit.
