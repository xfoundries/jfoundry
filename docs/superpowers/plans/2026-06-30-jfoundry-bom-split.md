# Jfoundry BOM Split Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Split jfoundry dependency management into framework-neutral and Spring-specific BOM layers, then make devcloud-ci import the Spring aggregate BOM.

**Architecture:** Keep `jfoundry-dependencies` framework-neutral by importing jfoundry module and Spring-independent third-party BOMs. Add Spring-specific BOMs for official Spring platform dependencies and Spring-bound third-party integrations, then aggregate them through `jfoundry-spring-dependencies`.

**Tech Stack:** Maven multi-module POMs, Spring Boot, Spring Cloud, Spring Cloud Alibaba, jfoundry.

---

### Task 1: Split jfoundry BOMs

**Files:**
- Modify: `/Users/huangxiao/Workspace/mine/jfoundry/pom.xml`
- Create: `/Users/huangxiao/Workspace/mine/jfoundry/jfoundry-dependencies/jfoundry-modules-dependencies/pom.xml`
- Create: `/Users/huangxiao/Workspace/mine/jfoundry/jfoundry-dependencies/jfoundry-third-party-dependencies/pom.xml`
- Create: `/Users/huangxiao/Workspace/mine/jfoundry/jfoundry-dependencies/jfoundry-spring-official-dependencies/pom.xml`
- Create: `/Users/huangxiao/Workspace/mine/jfoundry/jfoundry-dependencies/jfoundry-spring-integrations-dependencies/pom.xml`
- Create: `/Users/huangxiao/Workspace/mine/jfoundry/jfoundry-dependencies/jfoundry-spring-dependencies/pom.xml`
- Modify: `/Users/huangxiao/Workspace/mine/jfoundry/jfoundry-dependencies/pom.xml`

- [ ] Add child BOM modules under `jfoundry-dependencies`.
- [ ] Move `org.jfoundry:*` managed dependencies into `jfoundry-modules-dependencies`.
- [ ] Move Spring-independent external dependencies into `jfoundry-third-party-dependencies`.
- [ ] Move Spring official BOM imports into `jfoundry-spring-official-dependencies`.
- [ ] Move Spring-bound third-party dependencies into `jfoundry-spring-integration-dependencies`.
- [ ] Make `jfoundry-dependencies` import only framework-neutral BOMs.
- [ ] Make `jfoundry-spring-dependencies` import framework-neutral, Spring official, and Spring integration BOMs.

### Task 2: Verify jfoundry

**Files:**
- Verify Maven reactor and effective BOM output.

- [ ] Run `mvn -Pmy-nexus -pl jfoundry-dependencies help:effective-pom -Doutput=/tmp/jfoundry-dependencies-effective.xml -DskipTests`.
- [ ] Run `mvn -Pmy-nexus -pl jfoundry-dependencies/jfoundry-spring-dependencies help:effective-pom -Doutput=/tmp/jfoundry-spring-dependencies-effective.xml -DskipTests`.
- [ ] Run `mvn -Pmy-nexus validate -DskipTests`.
- [ ] Install the BOM modules locally with `mvn -Pmy-nexus -pl jfoundry-dependencies -am install -DskipTests`.

### Task 3: Update devcloud-ci BOM import

**Files:**
- Modify: `/Users/huangxiao/Workspace/mysoft/rdc_mks_aid/source-code/rdc/devcloud-ci-service/pom.xml`

- [ ] Remove direct Spring Boot, Spring Cloud, and Spring Cloud Alibaba BOM imports.
- [ ] Replace `jfoundry-dependencies` import with `jfoundry-spring-dependencies`.
- [ ] Remove unused Spring BOM version properties from business root POM.

### Task 4: Verify devcloud-ci and commit

**Files:**
- Verify Maven effective POM and architecture baseline test.

- [ ] Run effective POM checks for Spring Cloud and Alibaba dependency management.
- [ ] Run `mvn -Pmy-nexus -pl ci-start -am test -Dtest='CiArchitectureBaselineTest' -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false`.
- [ ] Commit jfoundry with an English Conventional Commit message.
- [ ] Commit devcloud-ci with the repository's Chinese commit style.
