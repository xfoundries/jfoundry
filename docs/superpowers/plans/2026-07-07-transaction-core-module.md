# Transaction Core Module Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Split framework-neutral application transaction contracts from `jfoundry-application-core` into a dedicated `jfoundry-transaction-core` module.

**Architecture:** Keep the public Java package `org.jfoundry.application.transaction` unchanged for source compatibility. Move only framework-neutral transaction contracts and tests into the new application-layer module; keep Spring integration in `jfoundry-transaction-spring`.

**Tech Stack:** Java 21, Maven multi-module reactor, JUnit Jupiter, AssertJ, Spring Boot auto-configuration tests.

---

### Task 1: Create Transaction Core Module

**Files:**
- Create: `jfoundry-application/jfoundry-transaction-core/pom.xml`
- Move: `jfoundry-application/jfoundry-application-core/src/main/java/org/jfoundry/application/transaction/*`
- Move: `jfoundry-application/jfoundry-application-core/src/test/java/org/jfoundry/application/transaction/*`
- Modify: `jfoundry-application/pom.xml`

- [x] Add `jfoundry-transaction-core` to the `jfoundry-application` reactor.
- [x] Move transaction source and test packages into the new module without changing package names.
- [x] Keep `jfoundry-application-core` focused on `ApplicationService` and application exceptions.

### Task 2: Rewire Dependencies

**Files:**
- Modify: `jfoundry-starters/jfoundry-application-starter/pom.xml`
- Modify: `jfoundry-spring/jfoundry-spring-runtime/jfoundry-transaction-spring/pom.xml`
- Modify: `jfoundry-spring/jfoundry-spring-boot-autoconfigure/pom.xml`
- Modify: `jfoundry-dependencies/jfoundry-modules-dependencies/pom.xml`

- [x] Add `jfoundry-transaction-core` to dependency management.
- [x] Make `jfoundry-application-starter` aggregate `jfoundry-transaction-core`.
- [x] Make Spring transaction runtime and auto-configuration depend on `jfoundry-transaction-core`.

### Task 3: Update Documentation

**Files:**
- Modify: `README.md`
- Modify: `docs/getting-started-for-business-projects.md`

- [x] Update module list so `jfoundry-application-core` and `jfoundry-transaction-core` have separate meanings.
- [x] Update TransactionRunner dependency wording.
- [x] Fix AI Agent plugin coordinates to `domain-architecture@xfoundries`.

### Task 4: Verify

**Commands:**
- `mvn -pl jfoundry-application/jfoundry-transaction-core,jfoundry-spring/jfoundry-spring-runtime/jfoundry-transaction-spring,jfoundry-spring/jfoundry-spring-boot-autoconfigure,jfoundry-starters/jfoundry-application-starter -am test -Pmy-nexus`
- `mvn validate -Pmy-nexus`

- [x] Confirm moved unit tests run in `jfoundry-transaction-core`.
- [x] Confirm Spring adapter and auto-configuration still compile and pass tests.
- [x] Confirm reactor module metadata validates.
