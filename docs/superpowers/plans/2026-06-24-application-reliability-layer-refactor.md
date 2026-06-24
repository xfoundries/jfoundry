# Application Reliability Layer Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move framework-neutral messaging/outbox/inbox contracts into a first-class application layer, rename reliability message stores consistently, and enforce the framework's own layered/hexagonal boundaries with annotations and ArchUnit tests.

**Architecture:** The project should follow `domain <- application <- infrastructure <- spring integration`. Application modules define ports and framework-neutral application services; infrastructure modules implement technical adapters; Spring modules only auto-configure and aggregate runtime wiring.

**Tech Stack:** Java 21, Maven multi-module reactor, jMolecules architecture stereotypes, ArchUnit, MyBatis-Plus, Spring Boot auto-configuration.

---

## File Structure

Create:

- `jfoundry-application/pom.xml`
- `jfoundry-application/jfoundry-messaging-core/**` by moving from `jfoundry-infrastructure/jfoundry-messaging-core/**`
- `jfoundry-application/jfoundry-outbox-core/**` by moving from `jfoundry-infrastructure/jfoundry-outbox-core/**`
- `jfoundry-application/jfoundry-inbox-core/**` by moving from `jfoundry-infrastructure/jfoundry-inbox-core/**`
- `jfoundry-application/jfoundry-messaging-core/src/main/java/org/jfoundry/application/messaging/package-info.java`
- `jfoundry-application/jfoundry-messaging-core/src/main/java/org/jfoundry/application/messaging/externalization/package-info.java`
- `jfoundry-application/jfoundry-outbox-core/src/main/java/org/jfoundry/application/outbox/package-info.java`
- `jfoundry-application/jfoundry-inbox-core/src/main/java/org/jfoundry/application/inbox/package-info.java`
- `jfoundry-test/src/main/java/org/jfoundry/test/archunit/FrameworkModuleRules.java`
- `jfoundry-test/src/test/java/org/jfoundry/test/archunit/FrameworkModuleRulesTest.java`
- `jfoundry-test/src/test/java/org/jfoundry/test/archunit/FrameworkArchitectureSelfTest.java`

Move/delete:

- Remove moved module entries from `jfoundry-infrastructure/pom.xml`.
- Add `jfoundry-application` to root `pom.xml`.

Rename classes:

- `OutboxEntry.java` -> `OutboxMessage.java`
- `OutboxStatus.java` -> `OutboxMessageStatus.java`
- `OutboxRepository.java` -> `OutboxMessageStore.java`
- `MybatisPlusOutboxRepository.java` -> `MybatisPlusOutboxMessageStore.java`
- `InboxRepository.java` -> `InboxMessageStore.java`
- `MybatisPlusInboxRepository.java` -> `MybatisPlusInboxMessageStore.java`

Modify all imports/usages in:

- `jfoundry-application/**`
- `jfoundry-infrastructure/**`
- `jfoundry-spring/**`
- `jfoundry-test/**`
- `README.md`
- `docs/transactional-outbox.md`
- `docs/framework-boundaries.md`
- `jfoundry-dependencies/pom.xml`
- module POM descriptions/comments

Do not change:

- `jfoundry-infrastructure/jfoundry-persistence-core/**`
- `jfoundry-infrastructure/jfoundry-persistence-mybatis-plus/**`
- database table names or migration SQL table names

---

## Task 1: Introduce `jfoundry-application` and Move Core Modules

**Files:**
- Create: `jfoundry-application/pom.xml`
- Modify: `pom.xml`
- Modify: `jfoundry-infrastructure/pom.xml`
- Move: `jfoundry-infrastructure/jfoundry-messaging-core` -> `jfoundry-application/jfoundry-messaging-core`
- Move: `jfoundry-infrastructure/jfoundry-outbox-core` -> `jfoundry-application/jfoundry-outbox-core`
- Move: `jfoundry-infrastructure/jfoundry-inbox-core` -> `jfoundry-application/jfoundry-inbox-core`
- Modify moved module parent relative paths.

- [ ] **Step 1: Create failing reactor check by updating root module first**

Edit `pom.xml` and add `jfoundry-application` between `jfoundry-architecture` and `jfoundry-infrastructure`:

```xml
    <modules>
        <module>jfoundry-dependencies</module>
        <module>jfoundry-domain</module>
        <module>jfoundry-architecture</module>
        <module>jfoundry-application</module>
        <module>jfoundry-infrastructure</module>
        <module>jfoundry-spring</module>
        <module>jfoundry-test</module>
    </modules>
```

- [ ] **Step 2: Run validate to verify red**

Run:

```bash
mvn validate
```

Expected: FAIL because `jfoundry-application/pom.xml` does not exist.

- [ ] **Step 3: Add application aggregator POM**

Create `jfoundry-application/pom.xml`:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.jfoundry</groupId>
        <artifactId>jfoundry-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>jfoundry-application</artifactId>
    <name>jfoundry-application</name>
    <description>Application-layer reliability contracts and framework-neutral application services.</description>
    <packaging>pom</packaging>

    <modules>
        <module>jfoundry-messaging-core</module>
        <module>jfoundry-outbox-core</module>
        <module>jfoundry-inbox-core</module>
    </modules>
</project>
```

- [ ] **Step 4: Move core module directories**

Run:

```bash
mkdir -p jfoundry-application
git mv jfoundry-infrastructure/jfoundry-messaging-core jfoundry-application/jfoundry-messaging-core
git mv jfoundry-infrastructure/jfoundry-outbox-core jfoundry-application/jfoundry-outbox-core
git mv jfoundry-infrastructure/jfoundry-inbox-core jfoundry-application/jfoundry-inbox-core
```

- [ ] **Step 5: Update moved module parent relative paths**

In these files:

- `jfoundry-application/jfoundry-messaging-core/pom.xml`
- `jfoundry-application/jfoundry-outbox-core/pom.xml`
- `jfoundry-application/jfoundry-inbox-core/pom.xml`

Change parent to:

```xml
    <parent>
        <groupId>org.jfoundry</groupId>
        <artifactId>jfoundry-application</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>
```

- [ ] **Step 6: Remove moved modules from infrastructure aggregator**

Edit `jfoundry-infrastructure/pom.xml` so `<modules>` no longer contains:

```xml
        <module>jfoundry-messaging-core</module>
        <module>jfoundry-inbox-core</module>
        <module>jfoundry-outbox-core</module>
```

Keep all adapter modules in infrastructure.

- [ ] **Step 7: Verify dependency artifact IDs intentionally remain unchanged**

Artifact coordinates remain unchanged:

```text
org.jfoundry:jfoundry-messaging-core
org.jfoundry:jfoundry-outbox-core
org.jfoundry:jfoundry-inbox-core
```

Run:

```bash
rg -n "<artifactId>jfoundry-(messaging-core|outbox-core|inbox-core)</artifactId>" jfoundry-infrastructure jfoundry-spring jfoundry-dependencies
```

Expected: command exits 0 and prints adapter, starter, and dependency-management references using the same artifact IDs. Do not change those artifact IDs in this task.

- [ ] **Step 8: Run application module tests**

Run:

```bash
mvn -pl jfoundry-application -am test
```

Expected: BUILD SUCCESS after path moves and parent updates.

- [ ] **Step 9: Commit**

Run:

```bash
git add pom.xml jfoundry-application jfoundry-infrastructure/pom.xml
git commit -m "refactor(application): introduce application reliability modules"
```

---

## Task 2: Move Packages to `org.jfoundry.application.*`

**Files:**
- Modify all Java source/test files under moved modules.
- Modify all imports in infrastructure, spring, and tests.

- [ ] **Step 1: Write failing import boundary search**

Run:

```bash
rg -n "org\\.jfoundry\\.infrastructure\\.(messaging|outbox\\.core|inbox)" jfoundry-application jfoundry-infrastructure jfoundry-spring jfoundry-test README.md docs
```

Expected: command exits 0 and prints old package references. This is the red state for package migration.

- [ ] **Step 2: Rename package declarations in moved modules**

Change package declarations:

```text
org.jfoundry.infrastructure.messaging
  -> org.jfoundry.application.messaging

org.jfoundry.infrastructure.messaging.externalization
  -> org.jfoundry.application.messaging.externalization

org.jfoundry.infrastructure.outbox.core
  -> org.jfoundry.application.outbox

org.jfoundry.infrastructure.inbox
  -> org.jfoundry.application.inbox
```

Move source files into matching directories using `git mv`. Example:

```bash
mkdir -p jfoundry-application/jfoundry-messaging-core/src/main/java/org/jfoundry/application
git mv jfoundry-application/jfoundry-messaging-core/src/main/java/org/jfoundry/infrastructure/messaging \
       jfoundry-application/jfoundry-messaging-core/src/main/java/org/jfoundry/application/messaging
```

Apply equivalent moves for:

```text
jfoundry-application/jfoundry-outbox-core/src/main/java/org/jfoundry/infrastructure/outbox/core
  -> jfoundry-application/jfoundry-outbox-core/src/main/java/org/jfoundry/application/outbox

jfoundry-application/jfoundry-inbox-core/src/main/java/org/jfoundry/infrastructure/inbox
  -> jfoundry-application/jfoundry-inbox-core/src/main/java/org/jfoundry/application/inbox
```

Also move test package directories under `src/test/java` for the moved modules.

- [ ] **Step 3: Update imports across the reactor**

Replace imports/usages:

```text
org.jfoundry.infrastructure.messaging
  -> org.jfoundry.application.messaging

org.jfoundry.infrastructure.messaging.externalization
  -> org.jfoundry.application.messaging.externalization

org.jfoundry.infrastructure.outbox.core
  -> org.jfoundry.application.outbox

org.jfoundry.infrastructure.inbox
  -> org.jfoundry.application.inbox
```

Do not alter infrastructure adapter package names.

- [ ] **Step 4: Add package-level architecture annotations for moved modules**

Create `jfoundry-application/jfoundry-messaging-core/src/main/java/org/jfoundry/application/messaging/package-info.java`:

```java
@org.jfoundry.architecture.layered.ApplicationLayer
@org.jfoundry.architecture.hexagonal.Application
package org.jfoundry.application.messaging;
```

Create `jfoundry-application/jfoundry-messaging-core/src/main/java/org/jfoundry/application/messaging/externalization/package-info.java`:

```java
@org.jfoundry.architecture.layered.ApplicationLayer
@org.jfoundry.architecture.hexagonal.Application
package org.jfoundry.application.messaging.externalization;
```

Create `jfoundry-application/jfoundry-outbox-core/src/main/java/org/jfoundry/application/outbox/package-info.java`:

```java
@org.jfoundry.architecture.layered.ApplicationLayer
@org.jfoundry.architecture.hexagonal.Application
package org.jfoundry.application.outbox;
```

Create `jfoundry-application/jfoundry-inbox-core/src/main/java/org/jfoundry/application/inbox/package-info.java`:

```java
@org.jfoundry.architecture.layered.ApplicationLayer
@org.jfoundry.architecture.hexagonal.Application
package org.jfoundry.application.inbox;
```

- [ ] **Step 5: Add architecture annotation dependencies to moved core modules**

In `jfoundry-application/jfoundry-messaging-core/pom.xml`, `jfoundry-application/jfoundry-outbox-core/pom.xml`, and `jfoundry-application/jfoundry-inbox-core/pom.xml`, add dependencies:

```xml
        <dependency>
            <groupId>org.jfoundry</groupId>
            <artifactId>jfoundry-layered</artifactId>
        </dependency>
        <dependency>
            <groupId>org.jfoundry</groupId>
            <artifactId>jfoundry-hexagonal</artifactId>
        </dependency>
```

- [ ] **Step 6: Run compile to verify imports**

Run:

```bash
mvn -pl jfoundry-application -am test
```

Expected: BUILD SUCCESS.

- [ ] **Step 7: Run old package search**

Run:

```bash
rg -n "org\\.jfoundry\\.infrastructure\\.(messaging|outbox\\.core|inbox)" jfoundry-application jfoundry-infrastructure jfoundry-spring jfoundry-test README.md docs/transactional-outbox.md docs/framework-boundaries.md
```

Expected: no results outside historical `docs/superpowers/*`.

- [ ] **Step 8: Commit**

Run:

```bash
git add jfoundry-application jfoundry-infrastructure jfoundry-spring jfoundry-test README.md docs/transactional-outbox.md docs/framework-boundaries.md
git commit -m "refactor(application): move reliability packages to application layer"
```

---

## Task 3: Rename Outbox API to Message Store Naming

**Files:**
- Rename: `jfoundry-application/jfoundry-outbox-core/src/main/java/org/jfoundry/application/outbox/OutboxEntry.java` -> `OutboxMessage.java`
- Rename: `OutboxRepository.java` -> `OutboxMessageStore.java`
- Rename: `OutboxStatus.java` -> `OutboxMessageStatus.java`
- Rename: `MybatisPlusOutboxRepository.java` -> `MybatisPlusOutboxMessageStore.java`
- Modify all outbox usages in infrastructure, spring, tests, docs.

- [ ] **Step 1: Write failing rename search**

Run:

```bash
rg -n "OutboxEntry|OutboxRepository|OutboxStatus|MybatisPlusOutboxRepository" jfoundry-application jfoundry-infrastructure jfoundry-spring README.md docs/transactional-outbox.md docs/framework-boundaries.md
```

Expected: many results. This is the red state for the outbox rename.

- [ ] **Step 2: Rename `OutboxEntry` to `OutboxMessage`**

Use `git mv`:

```bash
git mv jfoundry-application/jfoundry-outbox-core/src/main/java/org/jfoundry/application/outbox/OutboxEntry.java \
       jfoundry-application/jfoundry-outbox-core/src/main/java/org/jfoundry/application/outbox/OutboxMessage.java
git mv jfoundry-application/jfoundry-outbox-core/src/test/java/org/jfoundry/application/outbox/OutboxEntryTest.java \
       jfoundry-application/jfoundry-outbox-core/src/test/java/org/jfoundry/application/outbox/OutboxMessageTest.java
```

Update class names and factory methods:

```java
public class OutboxMessage {

    public static OutboxMessage newPending(String eventId, String topic, String payloadKey,
                                           String payloadType, String payloadJson, Instant occurredAt) {
        OutboxMessage message = new OutboxMessage();
        // keep existing field initialization
        return message;
    }
}
```

Keep method name `newPending`; do not change behavior.

- [ ] **Step 3: Rename `OutboxStatus` to `OutboxMessageStatus`**

Use `git mv`:

```bash
git mv jfoundry-application/jfoundry-outbox-core/src/main/java/org/jfoundry/application/outbox/OutboxStatus.java \
       jfoundry-application/jfoundry-outbox-core/src/main/java/org/jfoundry/application/outbox/OutboxMessageStatus.java
git mv jfoundry-application/jfoundry-outbox-core/src/test/java/org/jfoundry/application/outbox/OutboxStatusTest.java \
       jfoundry-application/jfoundry-outbox-core/src/test/java/org/jfoundry/application/outbox/OutboxMessageStatusTest.java
```

Update enum declaration:

```java
public enum OutboxMessageStatus {
    PENDING,
    DISPATCHING,
    PUBLISHED,
    FAILED,
    DEAD_LETTERED
}
```

- [ ] **Step 4: Rename `OutboxRepository` to `OutboxMessageStore`**

Use `git mv`:

```bash
git mv jfoundry-application/jfoundry-outbox-core/src/main/java/org/jfoundry/application/outbox/OutboxRepository.java \
       jfoundry-application/jfoundry-outbox-core/src/main/java/org/jfoundry/application/outbox/OutboxMessageStore.java
```

Update interface signature:

```java
public interface OutboxMessageStore {

    void append(OutboxMessage message);

    List<OutboxMessage> findDispatchable(int limit, Instant now);

    void markAsPublished(String eventId);

    void markAsFailed(String eventId, String errorMessage, int maxRetries, BackoffStrategy backoff);

    void reactivate(String eventId);

    List<OutboxMessage> claimDispatchable(int limit, String claimerId);

    int recoverStuckDispatching(Instant cutoff);

    int deleteByStatusAndOccurredAtBefore(OutboxMessageStatus status, Instant cutoff, int batchSize);
}
```

- [ ] **Step 5: Update `DefaultOutboxDispatchService`**

Change fields/constructor/use sites:

```java
private final OutboxMessageStore store;

public DefaultOutboxDispatchService(OutboxMessageStore store,
                                    MessageSender messageSender,
                                    int maxRetries,
                                    BackoffStrategy backoff,
                                    String claimerId) {
    this.store = store;
    this.messageSender = messageSender;
    this.maxRetries = maxRetries;
    this.backoff = backoff;
    this.claimerId = claimerId;
}

@Override
public void dispatch(int batchSize) {
    List<OutboxMessage> messages = store.claimDispatchable(batchSize, claimerId);
    for (OutboxMessage message : messages) {
        dispatchMessage(message);
    }
}
```

Rename private method `dispatchEntry` to `dispatchMessage`.

- [ ] **Step 6: Rename MyBatis outbox adapter**

Use `git mv`:

```bash
git mv jfoundry-infrastructure/jfoundry-outbox-mybatis-plus/src/main/java/org/jfoundry/infrastructure/outbox/mybatis/MybatisPlusOutboxRepository.java \
       jfoundry-infrastructure/jfoundry-outbox-mybatis-plus/src/main/java/org/jfoundry/infrastructure/outbox/mybatis/MybatisPlusOutboxMessageStore.java
```

Update class declaration:

```java
@org.jfoundry.architecture.layered.InfrastructureLayer
@org.jfoundry.architecture.hexagonal.SecondaryAdapter
public class MybatisPlusOutboxMessageStore implements OutboxMessageStore {
    // keep existing mapper logic
}
```

Update constructor name and imports. Do not extend `MybatisPlusRepository`.

- [ ] **Step 7: Rename OutboxData conversion methods**

In `OutboxData.java`, rename:

```java
public static OutboxData fromMessage(OutboxMessage message) { ... }

public static OutboxMessage toMessage(OutboxData data) { ... }
```

Update all calls:

```text
OutboxData.fromEntry(...) -> OutboxData.fromMessage(...)
OutboxData.toEntry(...)   -> OutboxData.toMessage(...)
```

- [ ] **Step 8: Update Spring outbox components and auto-config**

Update constructor parameters, bean names, and imports in:

- `DomainEventExternalizer`
- `ScheduledOutboxDispatcher`
- `OutboxMybatisPlusAutoConfiguration`
- `DomainEventExternalizerAutoConfiguration`
- `OutboxDispatcherAutoConfiguration`
- `OutboxRecoveryJob`
- `OutboxCleanupJob`
- `JobRunrDispatcherAutoConfiguration`

Use `OutboxMessageStore` for the port type and `MybatisPlusOutboxMessageStore` for the adapter implementation.

- [ ] **Step 9: Update outbox tests**

Rename test classes where they mention old public API:

```text
OutboxEntryTest -> OutboxMessageTest
OutboxStatusTest -> OutboxMessageStatusTest
MybatisPlusOutboxRepositoryTest -> MybatisPlusOutboxMessageStoreTest
MybatisPlusOutboxRepositoryFailFastTest -> MybatisPlusOutboxMessageStoreFailFastTest
```

Update test helper names:

```text
pendingEntry(...) -> pendingMessage(...)
RecordingRepository -> RecordingOutboxMessageStore
```

- [ ] **Step 10: Run outbox tests**

Run:

```bash
mvn -pl jfoundry-application/jfoundry-outbox-core -am test
mvn -pl jfoundry-infrastructure/jfoundry-outbox-mybatis-plus -am test
mvn -pl jfoundry-spring/jfoundry-autoconfigure -am -Dtest='*Outbox*,*DomainEventExternalization*' test
```

Expected: BUILD SUCCESS for each command.

- [ ] **Step 11: Run old outbox name search**

Run:

```bash
rg -n "OutboxEntry|OutboxRepository|OutboxStatus|MybatisPlusOutboxRepository" jfoundry-application jfoundry-infrastructure jfoundry-spring README.md docs/transactional-outbox.md docs/framework-boundaries.md
```

Expected: no results outside historical `docs/superpowers/*`.

- [ ] **Step 12: Commit**

Run:

```bash
git add jfoundry-application jfoundry-infrastructure jfoundry-spring README.md docs/transactional-outbox.md docs/framework-boundaries.md
git commit -m "refactor(outbox): rename outbox message store api"
```

---

## Task 4: Rename Inbox API to Message Store Naming

**Files:**
- Rename: `InboxRepository.java` -> `InboxMessageStore.java`
- Rename: `MybatisPlusInboxRepository.java` -> `MybatisPlusInboxMessageStore.java`
- Modify all inbox usages in application, infrastructure, spring, tests, docs.

- [ ] **Step 1: Write failing rename search**

Run:

```bash
rg -n "InboxRepository|MybatisPlusInboxRepository" jfoundry-application jfoundry-infrastructure jfoundry-spring README.md docs/transactional-outbox.md docs/framework-boundaries.md
```

Expected: many results. This is the red state for the inbox rename.

- [ ] **Step 2: Rename `InboxRepository` to `InboxMessageStore`**

Use `git mv`:

```bash
git mv jfoundry-application/jfoundry-inbox-core/src/main/java/org/jfoundry/application/inbox/InboxRepository.java \
       jfoundry-application/jfoundry-inbox-core/src/main/java/org/jfoundry/application/inbox/InboxMessageStore.java
```

Update interface declaration:

```java
@org.jfoundry.architecture.hexagonal.SecondaryPort
public interface InboxMessageStore {

    boolean isProcessed(String messageId, String consumerName);

    void markProcessed(String messageId, String consumerName);
}
```

- [ ] **Step 3: Update `InboxTemplate`**

Change constructor and field:

```java
private final InboxMessageStore store;

public InboxTemplate(InboxMessageStore store) {
    this.store = Objects.requireNonNull(store, "store must not be null");
}
```

Update use sites:

```java
if (store.isProcessed(messageId, consumerName)) {
    return false;
}
handler.handle();
store.markProcessed(messageId, consumerName);
return true;
```

- [ ] **Step 4: Rename MyBatis inbox adapter**

Use `git mv`:

```bash
git mv jfoundry-infrastructure/jfoundry-inbox-mybatis-plus/src/main/java/org/jfoundry/infrastructure/inbox/mybatis/MybatisPlusInboxRepository.java \
       jfoundry-infrastructure/jfoundry-inbox-mybatis-plus/src/main/java/org/jfoundry/infrastructure/inbox/mybatis/MybatisPlusInboxMessageStore.java
```

Update class declaration:

```java
@org.jfoundry.architecture.layered.InfrastructureLayer
@org.jfoundry.architecture.hexagonal.SecondaryAdapter
public class MybatisPlusInboxMessageStore implements InboxMessageStore {
    // keep existing mapper logic
}
```

Do not extend `MybatisPlusRepository`.

- [ ] **Step 5: Update Spring inbox auto-config**

Update:

- `InboxAutoConfiguration`
- `InboxMybatisPlusAutoConfiguration`
- `InboxAutoConfigurationTest`

Expected bean method signatures:

```java
@Bean
@ConditionalOnBean(InboxMessageStore.class)
@ConditionalOnMissingBean(InboxTemplate.class)
public InboxTemplate inboxTemplate(InboxMessageStore store) {
    return new InboxTemplate(store);
}

@Bean
@ConditionalOnMissingBean(InboxMessageStore.class)
public InboxMessageStore inboxMessageStore(InboxMessageMapper mapper) {
    return new MybatisPlusInboxMessageStore(mapper);
}
```

- [ ] **Step 6: Update inbox tests**

Rename test classes:

```text
MybatisPlusInboxRepositoryTest -> MybatisPlusInboxMessageStoreTest
```

Update helper class names:

```text
RecordingInboxRepository -> RecordingInboxMessageStore
```

- [ ] **Step 7: Run inbox tests**

Run:

```bash
mvn -pl jfoundry-application/jfoundry-inbox-core -am test
mvn -pl jfoundry-infrastructure/jfoundry-inbox-mybatis-plus -am test
mvn -pl jfoundry-spring/jfoundry-autoconfigure -am -Dtest=InboxAutoConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: BUILD SUCCESS for each command.

- [ ] **Step 8: Run old inbox name search**

Run:

```bash
rg -n "InboxRepository|MybatisPlusInboxRepository" jfoundry-application jfoundry-infrastructure jfoundry-spring README.md docs/transactional-outbox.md docs/framework-boundaries.md
```

Expected: no results outside historical `docs/superpowers/*`.

- [ ] **Step 9: Commit**

Run:

```bash
git add jfoundry-application jfoundry-infrastructure jfoundry-spring README.md docs/transactional-outbox.md docs/framework-boundaries.md
git commit -m "refactor(inbox): rename inbox message store api"
```

---

## Task 5: Add Framework Architecture Fitness Rules

**Files:**
- Create: `jfoundry-test/src/main/java/org/jfoundry/test/archunit/FrameworkModuleRules.java`
- Create: `jfoundry-test/src/test/java/org/jfoundry/test/archunit/FrameworkModuleRulesTest.java`
- Create: `jfoundry-test/src/test/java/org/jfoundry/test/archunit/FrameworkArchitectureSelfTest.java`
- Modify: `jfoundry-test/src/main/java/org/jfoundry/test/archunit/JFoundryRules.java`
- Add/modify package/class annotations in application and infrastructure adapters.

- [ ] **Step 1: Write failing self-test**

Create `jfoundry-test/src/test/java/org/jfoundry/test/archunit/FrameworkArchitectureSelfTest.java`:

```java
package org.jfoundry.test.archunit;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
        packages = "org.jfoundry",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class FrameworkArchitectureSelfTest {

    @ArchTest
    static final ArchRule domain_must_not_depend_on_outer_layers =
            FrameworkModuleRules.domain_must_not_depend_on_outer_layers;

    @ArchTest
    static final ArchRule application_must_not_depend_on_outer_layers =
            FrameworkModuleRules.application_must_not_depend_on_outer_layers;

    @ArchTest
    static final ArchRule infrastructure_must_not_depend_on_spring_autoconfigure =
            FrameworkModuleRules.infrastructure_must_not_depend_on_spring_autoconfigure;

    @ArchTest
    static final ArchRule application_store_ports_should_be_secondary_ports =
            FrameworkModuleRules.application_store_ports_should_be_secondary_ports;

    @ArchTest
    static final ArchRule message_sender_should_be_secondary_port =
            FrameworkModuleRules.message_sender_should_be_secondary_port;

    @ArchTest
    static final ArchRule payload_serializer_should_be_secondary_port =
            FrameworkModuleRules.payload_serializer_should_be_secondary_port;

    @ArchTest
    static final ArchRule infrastructure_mybatis_message_stores_should_be_secondary_adapters =
            FrameworkModuleRules.infrastructure_mybatis_message_stores_should_be_secondary_adapters;

    @ArchTest
    static final ArchRule kafka_message_sender_should_be_secondary_adapter =
            FrameworkModuleRules.kafka_message_sender_should_be_secondary_adapter;

    @ArchTest
    static final ArchRule jackson_payload_serializer_should_be_secondary_adapter =
            FrameworkModuleRules.jackson_payload_serializer_should_be_secondary_adapter;
}
```

- [ ] **Step 2: Run self-test to verify red**

Run:

```bash
mvn -pl jfoundry-test -am -Dtest=FrameworkArchitectureSelfTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: FAIL because `FrameworkModuleRules` does not exist.

- [ ] **Step 3: Implement `FrameworkModuleRules`**

Create `jfoundry-test/src/main/java/org/jfoundry/test/archunit/FrameworkModuleRules.java`:

```java
package org.jfoundry.test.archunit;

import com.tngtech.archunit.lang.ArchRule;
import org.jfoundry.architecture.hexagonal.SecondaryAdapter;
import org.jfoundry.architecture.hexagonal.SecondaryPort;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

public final class FrameworkModuleRules {

    private FrameworkModuleRules() {
    }

    public static final ArchRule domain_must_not_depend_on_outer_layers =
            noClasses()
                    .that().resideInAPackage("org.jfoundry.domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.jfoundry.application..",
                            "org.jfoundry.infrastructure..",
                            "org.jfoundry.autoconfigure..")
                    .allowEmptyShould(true)
                    .because("domain is the innermost layer");

    public static final ArchRule application_must_not_depend_on_outer_layers =
            noClasses()
                    .that().resideInAPackage("org.jfoundry.application..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.jfoundry.infrastructure..",
                            "org.jfoundry.autoconfigure..")
                    .allowEmptyShould(true)
                    .because("application defines ports and framework-neutral services");

    public static final ArchRule infrastructure_must_not_depend_on_spring_autoconfigure =
            noClasses()
                    .that().resideInAPackage("org.jfoundry.infrastructure..")
                    .should().dependOnClassesThat().resideInAPackage("org.jfoundry.autoconfigure..")
                    .allowEmptyShould(true)
                    .because("Spring Boot auto-configuration belongs to jfoundry-spring");

    public static final ArchRule application_store_ports_should_be_secondary_ports =
            classes()
                    .that().resideInAPackage("org.jfoundry.application..")
                    .and().haveSimpleNameEndingWith("Store")
                    .should().beAnnotatedWith(SecondaryPort.class)
                    .allowEmptyShould(true)
                    .because("application message stores are hexagonal secondary ports");

    public static final ArchRule message_sender_should_be_secondary_port =
            classes()
                    .that().resideInAPackage("org.jfoundry.application.messaging..")
                    .and().haveSimpleName("MessageSender")
                    .should().beAnnotatedWith(SecondaryPort.class)
                    .allowEmptyShould(true)
                    .because("MessageSender is the outbound broker port");

    public static final ArchRule payload_serializer_should_be_secondary_port =
            classes()
                    .that().resideInAPackage("org.jfoundry.application.messaging..")
                    .and().haveSimpleName("PayloadSerializer")
                    .should().beAnnotatedWith(SecondaryPort.class)
                    .allowEmptyShould(true)
                    .because("PayloadSerializer is the outbound serialization port");

    public static final ArchRule infrastructure_mybatis_message_stores_should_be_secondary_adapters =
            classes()
                    .that().resideInAPackage("org.jfoundry.infrastructure..mybatis..")
                    .and().haveSimpleNameEndingWith("MessageStore")
                    .should().beAnnotatedWith(SecondaryAdapter.class)
                    .allowEmptyShould(true)
                    .because("MyBatis message stores implement application secondary ports");

    public static final ArchRule kafka_message_sender_should_be_secondary_adapter =
            classes()
                    .that().resideInAPackage("org.jfoundry.infrastructure.messaging.kafka..")
                    .and().haveSimpleName("KafkaMessageSender")
                    .should().beAnnotatedWith(SecondaryAdapter.class)
                    .allowEmptyShould(true)
                    .because("KafkaMessageSender implements the MessageSender secondary port");

    public static final ArchRule jackson_payload_serializer_should_be_secondary_adapter =
            classes()
                    .that().resideInAPackage("org.jfoundry.infrastructure.messaging.jackson..")
                    .and().haveSimpleName("JacksonPayloadSerializer")
                    .should().beAnnotatedWith(SecondaryAdapter.class)
                    .allowEmptyShould(true)
                    .because("JacksonPayloadSerializer implements the PayloadSerializer secondary port");
}
```

- [ ] **Step 4: Add rule tests**

Create `jfoundry-test/src/test/java/org/jfoundry/test/archunit/FrameworkModuleRulesTest.java`:

```java
package org.jfoundry.test.archunit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FrameworkModuleRulesTest {

    @Test
    void declaresFrameworkBoundaryRules() {
        assertThat(FrameworkModuleRules.domain_must_not_depend_on_outer_layers).isNotNull();
        assertThat(FrameworkModuleRules.application_must_not_depend_on_outer_layers).isNotNull();
        assertThat(FrameworkModuleRules.infrastructure_must_not_depend_on_spring_autoconfigure).isNotNull();
        assertThat(FrameworkModuleRules.application_store_ports_should_be_secondary_ports).isNotNull();
        assertThat(FrameworkModuleRules.message_sender_should_be_secondary_port).isNotNull();
        assertThat(FrameworkModuleRules.payload_serializer_should_be_secondary_port).isNotNull();
        assertThat(FrameworkModuleRules.infrastructure_mybatis_message_stores_should_be_secondary_adapters).isNotNull();
        assertThat(FrameworkModuleRules.kafka_message_sender_should_be_secondary_adapter).isNotNull();
        assertThat(FrameworkModuleRules.jackson_payload_serializer_should_be_secondary_adapter).isNotNull();
    }
}
```

- [ ] **Step 5: Annotate application ports**

Add `@SecondaryPort` to:

- `jfoundry-application/jfoundry-messaging-core/src/main/java/org/jfoundry/application/messaging/MessageSender.java`
- `jfoundry-application/jfoundry-messaging-core/src/main/java/org/jfoundry/application/messaging/PayloadSerializer.java`
- `jfoundry-application/jfoundry-outbox-core/src/main/java/org/jfoundry/application/outbox/OutboxMessageStore.java`
- `jfoundry-application/jfoundry-inbox-core/src/main/java/org/jfoundry/application/inbox/InboxMessageStore.java`

Example:

```java
@org.jfoundry.architecture.hexagonal.SecondaryPort
public interface OutboxMessageStore {
}
```

- [ ] **Step 6: Annotate infrastructure adapters**

Add `@SecondaryAdapter` to:

- `MybatisPlusOutboxMessageStore`
- `MybatisPlusInboxMessageStore`
- `KafkaMessageSender`
- `JacksonPayloadSerializer`

Example:

```java
@org.jfoundry.architecture.hexagonal.SecondaryAdapter
public class KafkaMessageSender implements MessageSender {
}
```

If an adapter module does not already depend on `jfoundry-hexagonal`, add:

```xml
        <dependency>
            <groupId>org.jfoundry</groupId>
            <artifactId>jfoundry-hexagonal</artifactId>
        </dependency>
```

- [ ] **Step 7: Update `JFoundryRules` aggregation**

In `JFoundryRules.all()`, add:

```java
collected.addAll(publicStaticArchRules(FrameworkModuleRules.class));
```

Update the Javadoc list to include:

```java
///   <li>{@link FrameworkModuleRules} — jfoundry framework module boundary rules</li>
```

- [ ] **Step 8: Run framework architecture tests**

Run:

```bash
mvn -pl jfoundry-test -am test
```

Expected: BUILD SUCCESS.

- [ ] **Step 9: Commit**

Run:

```bash
git add jfoundry-application jfoundry-infrastructure jfoundry-test
git commit -m "test(architecture): enforce framework layer boundaries"
```

---

## Task 6: Update Docs, POM Descriptions, and Dependency Comments

**Files:**
- Modify: `README.md`
- Modify: `docs/transactional-outbox.md`
- Modify: `docs/framework-boundaries.md`
- Modify: `jfoundry-dependencies/pom.xml`
- Modify: module POM descriptions under `jfoundry-application/**`, `jfoundry-infrastructure/**`, `jfoundry-spring/**`

- [ ] **Step 1: Update README module list**

Ensure README shows:

```text
├── jfoundry-application                         应用层聚合
│   ├── jfoundry-messaging-core                  消息发送 / 事件外部化 application ports
│   ├── jfoundry-outbox-core                     Outbox message store port + 状态机 + dispatcher service
│   └── jfoundry-inbox-core                      Inbox message store port + InboxTemplate
├── jfoundry-infrastructure                      基础设施层聚合
│   ├── jfoundry-messaging-jackson               Jackson PayloadSerializer adapter
│   ├── jfoundry-messaging-kafka                 Kafka MessageSender adapter（可选）
│   ├── jfoundry-inbox-mybatis-plus              Inbox MyBatis-Plus store adapter
│   ├── jfoundry-outbox-mybatis-plus             Outbox MyBatis-Plus store adapter
```

Replace user-facing mentions:

```text
OutboxRepository -> OutboxMessageStore
InboxRepository  -> InboxMessageStore
OutboxEntry      -> OutboxMessage
OutboxStatus     -> OutboxMessageStatus
```

- [ ] **Step 2: Update `docs/transactional-outbox.md`**

Replace chain snippet with:

```text
Aggregate/Application Service
  -> DomainEventPublisher
  -> DomainEventExternalizer
  -> OutboxMessageStore
  -> jfoundry_outbox_event
  -> OutboxDispatcher
  -> MessageSender
  -> MQ / external system
```

Update configuration text:

```text
默认 MyBatis-Plus starter 会提供 `OutboxMessageStore`，表名默认为 `jfoundry_outbox_event`。
```

Update inbox text:

```text
jfoundry starter 会提供 `InboxTemplate` 和 MyBatis-Plus `InboxMessageStore`。
```

- [ ] **Step 3: Update `docs/framework-boundaries.md`**

Document the new boundary:

```text
`jfoundry-application` hosts application-facing reliability ports and framework-neutral services:

- `jfoundry-messaging-core`
- `jfoundry-outbox-core`
- `jfoundry-inbox-core`

`jfoundry-infrastructure` hosts technical adapters implementing or consuming application ports.
Outbox/Inbox data objects do not extend `BaseData`; their MyBatis stores use `BaseMapper` directly instead of `MybatisPlusRepository`.
```

- [ ] **Step 4: Update POM descriptions/comments**

Update descriptions:

```text
jfoundry-application: Application-layer reliability contracts and services.
jfoundry-infrastructure: Technical adapters for persistence, messaging, outbox, and inbox.
jfoundry-outbox-core: Application Outbox message model, store port, dispatcher service, status machine.
jfoundry-inbox-core: Application Inbox message model, store port, and idempotent execution template.
jfoundry-messaging-core: Application messaging ports and domain-event externalization metadata.
```

In `jfoundry-dependencies/pom.xml`, update comments mentioning `OutboxEntry` to `OutboxMessage`.

- [ ] **Step 5: Run docs-adjacent validation**

Run:

```bash
mvn validate
```

Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit**

Run:

```bash
git add README.md docs/transactional-outbox.md docs/framework-boundaries.md jfoundry-dependencies/pom.xml pom.xml jfoundry-application jfoundry-infrastructure jfoundry-spring
git commit -m "docs(application): document reliability layer boundaries"
```

---

## Task 7: Final Verification and Stale Name Audit

**Files:** no expected source changes unless verification finds missed references.

- [ ] **Step 1: Run stale package/name search**

Run:

```bash
rg -n "org\\.jfoundry\\.infrastructure\\.(messaging|outbox\\.core|inbox)|OutboxEntry|OutboxRepository|InboxRepository|MybatisPlusOutboxRepository|MybatisPlusInboxRepository|OutboxStatus" .
```

Expected:

- no production or current user-facing documentation references;
- matches under `docs/superpowers/specs` or `docs/superpowers/plans` may remain only when documenting historical old names or this migration plan.

- [ ] **Step 2: Run targeted module tests**

Run:

```bash
mvn -pl jfoundry-application -am test
mvn -pl jfoundry-infrastructure/jfoundry-outbox-mybatis-plus -am test
mvn -pl jfoundry-infrastructure/jfoundry-inbox-mybatis-plus -am test
mvn -pl jfoundry-spring/jfoundry-autoconfigure -am test
mvn -pl jfoundry-test -am test
```

Expected: BUILD SUCCESS for all commands.

- [ ] **Step 3: Run full test suite**

Run:

```bash
mvn test
```

Expected: BUILD SUCCESS.

- [ ] **Step 4: Inspect recent commits and clean working tree**

Run:

```bash
git status --short
git log --oneline -10
```

Expected:

- `git status --short` has no output.
- Recent commits include application module movement, outbox rename, inbox rename, architecture rules, and docs.

- [ ] **Step 5: Report verification results**

Summarize:

```text
Verification:
- mvn -pl jfoundry-application -am test: passed
- mvn -pl jfoundry-infrastructure/jfoundry-outbox-mybatis-plus -am test: passed
- mvn -pl jfoundry-infrastructure/jfoundry-inbox-mybatis-plus -am test: passed
- mvn -pl jfoundry-spring/jfoundry-autoconfigure -am test: passed
- mvn -pl jfoundry-test -am test: passed
- mvn test: passed
- stale name search: only historical docs/superpowers matches, if any
```
