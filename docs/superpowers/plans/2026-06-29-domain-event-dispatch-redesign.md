# 领域事件分发重构实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将领域事件上下文、Spring 应用事件发布、Outbox 记录拆成职责清晰的框架能力，并移除旧的耦合分发器和旧配置。

**Architecture:** 应用层只定义组合分发和分发端口；Spring Messaging 模块只负责 Spring `ApplicationEventPublisher`；Outbox Spring 模块只负责 `DomainEventOutboxRecorder`；Spring Boot auto-config 只负责条件装配。

**Tech Stack:** Java 21, Maven, Spring Boot auto-configuration, JUnit 5, AssertJ.

---

## File Structure

| File | Responsibility | Action |
|---|---|---|
| `jfoundry-application/jfoundry-messaging-core/.../CompositeDomainEventDispatcher.java` | 组合多个领域事件分发器 | Create |
| `jfoundry-infrastructure/jfoundry-messaging-spring/.../SpringApplicationEventDispatcher.java` | 发布 Spring 应用事件 | Create |
| `jfoundry-infrastructure/jfoundry-messaging-spring/.../SpringDomainEventDispatcher.java` | 旧耦合分发器 | Delete |
| `jfoundry-infrastructure/jfoundry-outbox-spring/.../OutboxDomainEventDispatcher.java` | 记录领域事件到 Outbox | Create |
| `jfoundry-spring/jfoundry-spring-boot-autoconfigure/.../DomainEventDispatchProperties.java` | 新 dispatch 配置模型 | Modify |
| `jfoundry-spring/jfoundry-spring-boot-autoconfigure/.../DomainEventDispatchAutoConfiguration.java` | 新自动装配条件 | Modify |
| `*Test.java` | 红绿测试覆盖新行为 | Modify/Create |

## Task 1: Add design and red tests

- [ ] Add Chinese Superpowers spec and plan under `docs/superpowers`.
- [ ] Update auto-configuration test to assert new dispatch properties and bean graph.
- [ ] Add unit tests for `CompositeDomainEventDispatcher`.
- [ ] Rename Spring dispatcher tests to target `SpringApplicationEventDispatcher`.
- [ ] Add Outbox dispatcher tests.
- [ ] Run targeted Maven test and confirm the expected red state.

## Task 2: Implement separated dispatchers

- [ ] Add `CompositeDomainEventDispatcher` in application messaging core.
- [ ] Add `SpringApplicationEventDispatcher` in Spring messaging infrastructure.
- [ ] Add `OutboxDomainEventDispatcher` in Outbox Spring infrastructure.
- [ ] Delete `SpringDomainEventDispatcher`.
- [ ] Run dispatcher module tests.

## Task 3: Rewrite auto-configuration

- [ ] Replace top-level `enabled` property with nested `dispatch` properties.
- [ ] Remove auto-configuration class condition on `jfoundry.domain.event.enabled`.
- [ ] Always configure `DomainEventScope` and `DomainEventContext`.
- [ ] Conditionally configure Spring and Outbox concrete dispatchers.
- [ ] Configure composite dispatcher, interceptor and advisor only when dispatch is enabled and at least one concrete dispatcher exists.
- [ ] Run auto-configuration tests.

## Task 4: Cleanup and verify

- [ ] Search for `SpringDomainEventDispatcher` and remove all references.
- [ ] Search for `jfoundry.domain.event.enabled` and remove all references.
- [ ] Run:

```bash
mvn -pl jfoundry-application/jfoundry-messaging-core,jfoundry-infrastructure/jfoundry-messaging-spring,jfoundry-infrastructure/jfoundry-outbox-spring,jfoundry-spring/jfoundry-spring-boot-autoconfigure -am test
mvn validate
rg -n "SpringDomainEventDispatcher|jfoundry.domain.event.enabled" .
```

## Task 5: Commit

- [ ] Inspect `git diff`.
- [ ] Stage only intended files, excluding `graphify-out/`.
- [ ] Commit with a linear commit on `main`, no merge commit.
