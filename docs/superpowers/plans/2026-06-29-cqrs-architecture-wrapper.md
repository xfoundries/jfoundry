# CQRS Architecture Wrapper Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a JFoundry-owned CQRS architecture wrapper module over jMolecules.

**Architecture:** Mirror the existing `jfoundry-hexagonal` and `jfoundry-onion` modules. The new `jfoundry-cqrs` module provides annotations only and has no runtime dispatcher or bus behavior.

**Tech Stack:** Java 21, Maven, jMolecules CQRS Architecture, JUnit 5, AssertJ.

---

### Task 1: Add Failing CQRS Stereotype Tests

**Files:**
- Create: `jfoundry-architecture/jfoundry-cqrs/pom.xml`
- Create: `jfoundry-architecture/jfoundry-cqrs/src/test/java/org/jfoundry/architecture/cqrs/CqrsStereotypesTest.java`

- [ ] **Step 1: Write tests**

Create tests that assert `Command`, `CommandHandler`, `CommandDispatcher`, and `QueryModel` are meta-annotated with jMolecules CQRS annotations and expose expected attributes.

- [ ] **Step 2: Run tests to verify RED**

Run: `mvn -pl jfoundry-architecture/jfoundry-cqrs -am test`

Expected: FAIL because the module is not listed in `jfoundry-architecture/pom.xml` or production annotations do not exist yet.

### Task 2: Implement CQRS Wrapper Module

**Files:**
- Modify: `jfoundry-architecture/pom.xml`
- Create: `jfoundry-architecture/jfoundry-cqrs/src/main/java/org/jfoundry/architecture/cqrs/Command.java`
- Create: `jfoundry-architecture/jfoundry-cqrs/src/main/java/org/jfoundry/architecture/cqrs/CommandHandler.java`
- Create: `jfoundry-architecture/jfoundry-cqrs/src/main/java/org/jfoundry/architecture/cqrs/CommandDispatcher.java`
- Create: `jfoundry-architecture/jfoundry-cqrs/src/main/java/org/jfoundry/architecture/cqrs/QueryModel.java`

- [ ] **Step 1: Add module to architecture aggregator**

Add `<module>jfoundry-cqrs</module>`.

- [ ] **Step 2: Add annotations**

Each annotation follows the existing JFoundry architecture wrapper style: `RUNTIME`, `TYPE`, `DOCUMENTED`, and meta-annotated with matching jMolecules annotation.

- [ ] **Step 3: Run focused tests**

Run: `mvn -pl jfoundry-architecture/jfoundry-cqrs -am test`

Expected: PASS.

### Task 3: Validate and Commit

**Files:**
- All changed jfoundry files.

- [ ] **Step 1: Run validation**

Run: `mvn -Pmy-nexus validate`

Expected: PASS.

- [ ] **Step 2: Commit**

Run:

```bash
git add docs/superpowers/specs/2026-06-29-cqrs-architecture-wrapper-design.md docs/superpowers/plans/2026-06-29-cqrs-architecture-wrapper.md jfoundry-architecture/pom.xml jfoundry-architecture/jfoundry-cqrs
git commit -m "feat(architecture): add cqrs stereotypes"
```
