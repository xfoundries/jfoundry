# Exception ProblemDetail WebMVC Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a compact jfoundry exception model and Spring MVC ProblemDetail mapping for new jfoundry applications.

**Architecture:** Domain exceptions stay in `jfoundry-domain`. Application/use-case exceptions stay in `jfoundry-application-core`. Spring MVC maps those core exceptions to RFC 9457 `ProblemDetail` in `jfoundry-webmvc-spring`, with Boot wiring in `jfoundry-spring-boot-autoconfigure` and dependency aggregation in `jfoundry-webmvc-spring-boot-starter`.

**Tech Stack:** Java 21, Maven, Spring Framework MVC, Spring Boot auto-configuration, JUnit 5, AssertJ.

---

### Task 1: Core Exception Model

**Files:**
- Create: `jfoundry-domain/src/main/java/org/jfoundry/domain/exception/DomainException.java`
- Create: `jfoundry-domain/src/main/java/org/jfoundry/domain/exception/DomainRuleViolationException.java`
- Create: `jfoundry-domain/src/main/java/org/jfoundry/domain/exception/DomainStateException.java`
- Create: `jfoundry-domain/src/test/java/org/jfoundry/domain/exception/DomainExceptionTest.java`
- Create: `jfoundry-application/jfoundry-application-core/src/main/java/org/jfoundry/application/exception/ApplicationException.java`
- Create: `jfoundry-application/jfoundry-application-core/src/main/java/org/jfoundry/application/exception/InvalidArgumentException.java`
- Create: `jfoundry-application/jfoundry-application-core/src/main/java/org/jfoundry/application/exception/NotFoundException.java`
- Create: `jfoundry-application/jfoundry-application-core/src/main/java/org/jfoundry/application/exception/ConflictException.java`
- Create: `jfoundry-application/jfoundry-application-core/src/main/java/org/jfoundry/application/exception/ExternalAccessException.java`
- Create: `jfoundry-application/jfoundry-application-core/src/test/java/org/jfoundry/application/exception/ApplicationExceptionTest.java`

- [ ] Write failing tests that verify each exception preserves message and cause.
- [ ] Run `mvn -pl jfoundry-domain,jfoundry-application/jfoundry-application-core test -Pmy-nexus` and confirm compilation fails because types do not exist.
- [ ] Add the exception classes with English Javadocs and minimal constructors.
- [ ] Re-run the same Maven command and confirm it passes.

### Task 2: Spring MVC ProblemDetail Runtime

**Files:**
- Modify: `jfoundry-spring/jfoundry-spring-runtime/pom.xml`
- Create: `jfoundry-spring/jfoundry-spring-runtime/jfoundry-webmvc-spring/pom.xml`
- Create: `jfoundry-spring/jfoundry-spring-runtime/jfoundry-webmvc-spring/src/main/java/org/jfoundry/webmvc/spring/ProblemCode.java`
- Create: `jfoundry-spring/jfoundry-spring-runtime/jfoundry-webmvc-spring/src/main/java/org/jfoundry/webmvc/spring/CoreProblemCode.java`
- Create: `jfoundry-spring/jfoundry-spring-runtime/jfoundry-webmvc-spring/src/main/java/org/jfoundry/webmvc/spring/CoreProblemCodeResolver.java`
- Create: `jfoundry-spring/jfoundry-spring-runtime/jfoundry-webmvc-spring/src/main/java/org/jfoundry/webmvc/spring/HttpProblemCode.java`
- Create: `jfoundry-spring/jfoundry-spring-runtime/jfoundry-webmvc-spring/src/main/java/org/jfoundry/webmvc/spring/HttpProblemCodeResolver.java`
- Create: `jfoundry-spring/jfoundry-spring-runtime/jfoundry-webmvc-spring/src/main/java/org/jfoundry/webmvc/spring/ProblemDetails.java`
- Create: `jfoundry-spring/jfoundry-spring-runtime/jfoundry-webmvc-spring/src/main/java/org/jfoundry/webmvc/spring/ProblemDetailExceptionHandler.java`
- Create: `jfoundry-spring/jfoundry-spring-runtime/jfoundry-webmvc-spring/src/test/java/org/jfoundry/webmvc/spring/CoreProblemCodeResolverTest.java`
- Create: `jfoundry-spring/jfoundry-spring-runtime/jfoundry-webmvc-spring/src/test/java/org/jfoundry/webmvc/spring/HttpProblemCodeResolverTest.java`
- Create: `jfoundry-spring/jfoundry-spring-runtime/jfoundry-webmvc-spring/src/test/java/org/jfoundry/webmvc/spring/ProblemDetailExceptionHandlerTest.java`

- [ ] Write failing tests for core exception to HTTP status and ProblemDetail property mapping.
- [ ] Run `mvn -pl jfoundry-spring/jfoundry-spring-runtime/jfoundry-webmvc-spring -am test -Pmy-nexus` and confirm the module is missing.
- [ ] Add the runtime module and classes.
- [ ] Re-run the same Maven command and confirm it passes.

### Task 3: Spring Boot Auto-Configuration and Starter

**Files:**
- Modify: `jfoundry-spring/jfoundry-spring-boot-autoconfigure/pom.xml`
- Create: `jfoundry-spring/jfoundry-spring-boot-autoconfigure/src/main/java/org/jfoundry/autoconfigure/webmvc/WebMvcProblemDetailAutoConfiguration.java`
- Modify: `jfoundry-spring/jfoundry-spring-boot-autoconfigure/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Create: `jfoundry-spring/jfoundry-spring-boot-autoconfigure/src/test/java/org/jfoundry/autoconfigure/webmvc/WebMvcProblemDetailAutoConfigurationTest.java`
- Modify: `jfoundry-spring/jfoundry-spring-boot-starters/pom.xml`
- Create: `jfoundry-spring/jfoundry-spring-boot-starters/jfoundry-webmvc-spring-boot-starter/pom.xml`
- Modify: `jfoundry-dependencies/jfoundry-modules-dependencies/pom.xml`

- [ ] Write failing auto-configuration tests for default handler creation and user bean backoff.
- [ ] Add optional autoconfigure dependency on `jfoundry-webmvc-spring`.
- [ ] Add the auto-configuration class and imports entry.
- [ ] Add the WebMVC starter and BOM entries.
- [ ] Run `mvn -pl jfoundry-spring/jfoundry-spring-boot-autoconfigure,jfoundry-spring/jfoundry-spring-boot-starters/jfoundry-webmvc-spring-boot-starter -am test -Pmy-nexus`.

### Task 4: Skill Guidance Update

**Files:**
- Modify: `/Users/huangxiao/Workspace/mine/software-architecture-skills/skills/use-jfoundry/SKILL.md`
- Modify: `/Users/huangxiao/Workspace/mine/software-architecture-skills/skills/use-jfoundry/references/dependencies.md`
- Modify: `/Users/huangxiao/Workspace/mine/software-architecture-skills/skills/use-jfoundry/references/architecture.md`

- [ ] Document the compact exception model and `ProblemDetail` WebMVC default.
- [ ] Document `jfoundry-webmvc-spring-boot-starter` for Spring MVC web applications.
- [ ] Run a text search to confirm old guidance does not recommend generic BusinessException or custom HTTP response wrappers.

### Task 5: Final Verification

**Files:** all files above.

- [ ] Run targeted jfoundry Maven tests with `-Pmy-nexus`.
- [ ] Run `mvn validate -Pmy-nexus`.
- [ ] Review git diff for jfoundry and software-architecture-skills.
