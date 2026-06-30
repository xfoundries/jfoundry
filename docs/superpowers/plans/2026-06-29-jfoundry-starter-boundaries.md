# JFoundry Starter Boundaries Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rework jfoundry starter boundaries so domain, application, infrastructure, and Spring Boot assembly dependencies are explicit, convenient, and semantically clean.

**Architecture:** Non-Spring starter modules live under `jfoundry-starters` and represent business-layer capability bundles. Spring Boot starter modules stay under `jfoundry-spring` and represent runtime auto-configuration entry points. Technical implementation modules such as `jfoundry-persistence-mybatis-plus` remain clean implementation modules and do not propagate architecture stereotypes.

**Tech Stack:** Java 21, Maven multi-module, Spring Boot 3.5.x, MyBatis-Plus, jMolecules, jfoundry architecture/domain/application/infrastructure modules.

---

## Target Dependency Model

```text
jfoundry-domain-starter
  -> jfoundry-domain
  -> jfoundry-hexagonal
  -> jfoundry-onion

jfoundry-application-starter
  -> jfoundry-domain-starter
  -> jfoundry-application-core
  -> jfoundry-cqrs

jfoundry-infrastructure-mybatis-plus-starter
  -> jfoundry-domain-starter
  -> jfoundry-persistence-mybatis-plus
  -> mybatis-plus-jsqlparser

jfoundry-mybatis-plus-spring-boot-starter
  -> jfoundry-spring-boot-starter
  -> jfoundry-infrastructure-mybatis-plus-starter
  -> mybatis-plus-spring-boot3-starter
```

Business project target:

```text
ci-domain
  -> jfoundry-domain-starter

ci-app
  -> jfoundry-application-starter

ci-adapter
  -> ci-app
  -> no direct jfoundry architecture dependency

ci-infrastructure
  -> ci-domain
  -> ci-app
  -> jfoundry-infrastructure-mybatis-plus-starter

ci-start
  -> ci-adapter
  -> ci-infrastructure
  -> jfoundry-spring-boot-starter
  -> jfoundry-mybatis-plus-spring-boot-starter
```

---

### Task 1: Add Non-Spring Starter Aggregator

**Files:**
- Modify: `/Users/huangxiao/Workspace/mine/jfoundry/pom.xml`
- Create: `/Users/huangxiao/Workspace/mine/jfoundry/jfoundry-starters/pom.xml`

- [ ] **Step 1: Add `jfoundry-starters` to the root reactor**

In `/Users/huangxiao/Workspace/mine/jfoundry/pom.xml`, add the module after `jfoundry-infrastructure` and before `jfoundry-spring`:

```xml
<module>jfoundry-starters</module>
```

- [ ] **Step 2: Create the starter parent POM**

Create `/Users/huangxiao/Workspace/mine/jfoundry/jfoundry-starters/pom.xml`:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.jfoundry</groupId>
        <artifactId>jfoundry-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>jfoundry-starters</artifactId>
    <packaging>pom</packaging>
    <name>jfoundry-starters</name>
    <description>Non-Spring starter modules for jfoundry business-layer capabilities.</description>

    <modules>
        <module>jfoundry-domain-starter</module>
        <module>jfoundry-application-starter</module>
        <module>jfoundry-infrastructure-mybatis-plus-starter</module>
    </modules>
</project>
```

- [ ] **Step 3: Verify reactor sees the new parent**

Run:

```bash
mvn -Pmy-nexus -pl jfoundry-starters -am validate
```

Expected: `BUILD SUCCESS`.

---

### Task 2: Add Domain And Application Starters

**Files:**
- Create: `/Users/huangxiao/Workspace/mine/jfoundry/jfoundry-starters/jfoundry-domain-starter/pom.xml`
- Create: `/Users/huangxiao/Workspace/mine/jfoundry/jfoundry-starters/jfoundry-application-starter/pom.xml`
- Modify: `/Users/huangxiao/Workspace/mine/jfoundry/jfoundry-dependencies/pom.xml`
- Modify: `/Users/huangxiao/Workspace/mine/jfoundry/pom.xml`

- [ ] **Step 1: Create `jfoundry-domain-starter`**

Create `/Users/huangxiao/Workspace/mine/jfoundry/jfoundry-starters/jfoundry-domain-starter/pom.xml`:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.jfoundry</groupId>
        <artifactId>jfoundry-starters</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>jfoundry-domain-starter</artifactId>
    <name>jfoundry-domain-starter</name>
    <description>Starter for jfoundry domain modeling and architecture boundary stereotypes.</description>

    <dependencies>
        <dependency>
            <groupId>org.jfoundry</groupId>
            <artifactId>jfoundry-domain</artifactId>
        </dependency>
        <dependency>
            <groupId>org.jfoundry</groupId>
            <artifactId>jfoundry-hexagonal</artifactId>
        </dependency>
        <dependency>
            <groupId>org.jfoundry</groupId>
            <artifactId>jfoundry-onion</artifactId>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: Create `jfoundry-application-starter`**

Create `/Users/huangxiao/Workspace/mine/jfoundry/jfoundry-starters/jfoundry-application-starter/pom.xml`:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.jfoundry</groupId>
        <artifactId>jfoundry-starters</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>jfoundry-application-starter</artifactId>
    <name>jfoundry-application-starter</name>
    <description>Starter for jfoundry application-layer contracts, CQRS stereotypes, and domain architecture APIs.</description>

    <dependencies>
        <dependency>
            <groupId>org.jfoundry</groupId>
            <artifactId>jfoundry-domain-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.jfoundry</groupId>
            <artifactId>jfoundry-application-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.jfoundry</groupId>
            <artifactId>jfoundry-cqrs</artifactId>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 3: Add dependency management entries**

In `/Users/huangxiao/Workspace/mine/jfoundry/jfoundry-dependencies/pom.xml`, add managed entries for:

```xml
<dependency>
    <groupId>org.jfoundry</groupId>
    <artifactId>jfoundry-domain-starter</artifactId>
    <version>${project.version}</version>
</dependency>
<dependency>
    <groupId>org.jfoundry</groupId>
    <artifactId>jfoundry-application-starter</artifactId>
    <version>${project.version}</version>
</dependency>
```

- [ ] **Step 4: Add root dependency management entries**

In `/Users/huangxiao/Workspace/mine/jfoundry/pom.xml`, add the same two managed entries so local modules can resolve them without versions.

- [ ] **Step 5: Verify the two starters**

Run:

```bash
mvn -Pmy-nexus -pl jfoundry-starters/jfoundry-domain-starter,jfoundry-starters/jfoundry-application-starter -am test -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: `BUILD SUCCESS`.

---

### Task 3: Add Infrastructure MyBatis-Plus Starter

**Files:**
- Create: `/Users/huangxiao/Workspace/mine/jfoundry/jfoundry-starters/jfoundry-infrastructure-mybatis-plus-starter/pom.xml`
- Modify: `/Users/huangxiao/Workspace/mine/jfoundry/jfoundry-dependencies/pom.xml`
- Modify: `/Users/huangxiao/Workspace/mine/jfoundry/pom.xml`
- Modify: `/Users/huangxiao/Workspace/mine/jfoundry/jfoundry-infrastructure/jfoundry-persistence-mybatis-plus/pom.xml`

- [ ] **Step 1: Remove accidental architecture dependency from persistence implementation**

In `/Users/huangxiao/Workspace/mine/jfoundry/jfoundry-infrastructure/jfoundry-persistence-mybatis-plus/pom.xml`, remove:

```xml
<dependency>
    <groupId>org.jmolecules</groupId>
    <artifactId>jmolecules-onion-architecture</artifactId>
</dependency>
```

- [ ] **Step 2: Create `jfoundry-infrastructure-mybatis-plus-starter`**

Create `/Users/huangxiao/Workspace/mine/jfoundry/jfoundry-starters/jfoundry-infrastructure-mybatis-plus-starter/pom.xml`:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.jfoundry</groupId>
        <artifactId>jfoundry-starters</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>jfoundry-infrastructure-mybatis-plus-starter</artifactId>
    <name>jfoundry-infrastructure-mybatis-plus-starter</name>
    <description>Starter for infrastructure-layer MyBatis-Plus persistence with jfoundry.</description>

    <dependencies>
        <dependency>
            <groupId>org.jfoundry</groupId>
            <artifactId>jfoundry-domain-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.jfoundry</groupId>
            <artifactId>jfoundry-persistence-mybatis-plus</artifactId>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-jsqlparser</artifactId>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 3: Add dependency management entries**

Add `jfoundry-infrastructure-mybatis-plus-starter` to both:

```text
/Users/huangxiao/Workspace/mine/jfoundry/jfoundry-dependencies/pom.xml
/Users/huangxiao/Workspace/mine/jfoundry/pom.xml
```

Managed entry:

```xml
<dependency>
    <groupId>org.jfoundry</groupId>
    <artifactId>jfoundry-infrastructure-mybatis-plus-starter</artifactId>
    <version>${project.version}</version>
</dependency>
```

- [ ] **Step 4: Verify infrastructure starter**

Run:

```bash
mvn -Pmy-nexus -pl jfoundry-starters/jfoundry-infrastructure-mybatis-plus-starter -am test -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: `BUILD SUCCESS`.

---

### Task 4: Rewire Spring Boot MyBatis-Plus Starter

**Files:**
- Modify: `/Users/huangxiao/Workspace/mine/jfoundry/jfoundry-spring/jfoundry-spring-boot-starters/jfoundry-mybatis-plus-spring-boot-starter/pom.xml`
- Modify: `/Users/huangxiao/Workspace/mine/jfoundry/docs/framework-boundaries.md`
- Modify: `/Users/huangxiao/Workspace/mine/jfoundry/README.md`

- [ ] **Step 1: Replace direct persistence and architecture dependencies**

In `/Users/huangxiao/Workspace/mine/jfoundry/jfoundry-spring/jfoundry-spring-boot-starters/jfoundry-mybatis-plus-spring-boot-starter/pom.xml`, dependencies should be:

```xml
<dependencies>
    <dependency>
        <groupId>org.jfoundry</groupId>
        <artifactId>jfoundry-spring-boot-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>org.jfoundry</groupId>
        <artifactId>jfoundry-infrastructure-mybatis-plus-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
    </dependency>
</dependencies>
```

- [ ] **Step 2: Document starter meanings**

In `/Users/huangxiao/Workspace/mine/jfoundry/docs/framework-boundaries.md`, document:

```markdown
- `jfoundry-domain-starter`: aggregates domain modeling and architecture boundary stereotypes.
- `jfoundry-application-starter`: aggregates domain starter, application core, and CQRS stereotypes.
- `jfoundry-infrastructure-mybatis-plus-starter`: aggregates infrastructure-layer MyBatis-Plus persistence APIs and common SQL parser support.
- `jfoundry-mybatis-plus-spring-boot-starter`: Spring Boot runtime assembly for MyBatis-Plus persistence.
```

- [ ] **Step 3: Update README dependency examples**

In `/Users/huangxiao/Workspace/mine/jfoundry/README.md`, show:

```xml
<dependency>
    <groupId>org.jfoundry</groupId>
    <artifactId>jfoundry-domain-starter</artifactId>
</dependency>
<dependency>
    <groupId>org.jfoundry</groupId>
    <artifactId>jfoundry-application-starter</artifactId>
</dependency>
<dependency>
    <groupId>org.jfoundry</groupId>
    <artifactId>jfoundry-infrastructure-mybatis-plus-starter</artifactId>
</dependency>
<dependency>
    <groupId>org.jfoundry</groupId>
    <artifactId>jfoundry-mybatis-plus-spring-boot-starter</artifactId>
</dependency>
```

- [ ] **Step 4: Verify Spring Boot starter dependency tree**

Run:

```bash
mvn -Pmy-nexus -pl jfoundry-spring/jfoundry-spring-boot-starters/jfoundry-mybatis-plus-spring-boot-starter dependency:tree -Dincludes=org.jfoundry
```

Expected tree contains `jfoundry-infrastructure-mybatis-plus-starter` and does not contain `jfoundry-architecture-starter`.

---

### Task 5: Update DevCloud CI Dependencies

**Files:**
- Modify: `/Users/huangxiao/Workspace/mysoft/rdc_mks_aid/source-code/rdc/devcloud-ci-service/ci-domain/pom.xml`
- Modify: `/Users/huangxiao/Workspace/mysoft/rdc_mks_aid/source-code/rdc/devcloud-ci-service/ci-app/pom.xml`
- Modify: `/Users/huangxiao/Workspace/mysoft/rdc_mks_aid/source-code/rdc/devcloud-ci-service/ci-adapter/pom.xml`
- Modify: `/Users/huangxiao/Workspace/mysoft/rdc_mks_aid/source-code/rdc/devcloud-ci-service/ci-infrastructure/pom.xml`
- Modify: `/Users/huangxiao/Workspace/mysoft/rdc_mks_aid/source-code/rdc/devcloud-ci-service/ci-start/pom.xml`

- [ ] **Step 1: Update `ci-domain`**

Replace:

```xml
<dependency>
    <groupId>org.jfoundry</groupId>
    <artifactId>jfoundry-domain</artifactId>
</dependency>
```

with:

```xml
<dependency>
    <groupId>org.jfoundry</groupId>
    <artifactId>jfoundry-domain-starter</artifactId>
</dependency>
```

- [ ] **Step 2: Update `ci-app`**

Replace:

```xml
<dependency>
    <groupId>org.jfoundry</groupId>
    <artifactId>jfoundry-architecture-starter</artifactId>
</dependency>
```

with:

```xml
<dependency>
    <groupId>org.jfoundry</groupId>
    <artifactId>jfoundry-application-starter</artifactId>
</dependency>
```

- [ ] **Step 3: Update `ci-adapter`**

Remove the direct `jfoundry-architecture-starter` dependency. `ci-adapter` receives hexagonal annotations through `ci-app`.

- [ ] **Step 4: Update `ci-infrastructure`**

Replace:

```xml
<dependency>
    <groupId>org.jfoundry</groupId>
    <artifactId>jfoundry-mybatis-plus-spring-boot-starter</artifactId>
</dependency>
```

with:

```xml
<dependency>
    <groupId>org.jfoundry</groupId>
    <artifactId>jfoundry-infrastructure-mybatis-plus-starter</artifactId>
</dependency>
```

Remove direct `mybatis-plus-jsqlparser` if still present, because it is provided by `jfoundry-infrastructure-mybatis-plus-starter`.

- [ ] **Step 5: Update `ci-start`**

Ensure `ci-start` has both runtime starters:

```xml
<dependency>
    <groupId>org.jfoundry</groupId>
    <artifactId>jfoundry-spring-boot-starter</artifactId>
</dependency>
<dependency>
    <groupId>org.jfoundry</groupId>
    <artifactId>jfoundry-mybatis-plus-spring-boot-starter</artifactId>
</dependency>
```

- [ ] **Step 6: Verify business dependency tree**

Run:

```bash
mvn -Pmy-nexus -pl ci-infrastructure dependency:tree -Dincludes=org.jfoundry,com.baomidou:mybatis-plus-jsqlparser
mvn -Pmy-nexus -pl ci-start dependency:tree -Dincludes=org.jfoundry
```

Expected:

```text
ci-infrastructure -> jfoundry-infrastructure-mybatis-plus-starter
ci-start -> jfoundry-mybatis-plus-spring-boot-starter
```

---

### Task 6: Full Verification And Commits

**Files:**
- All files changed in Tasks 1-5.

- [ ] **Step 1: Run jfoundry focused verification**

Run:

```bash
mvn -Pmy-nexus -pl jfoundry-starters,jfoundry-spring/jfoundry-spring-boot-starters/jfoundry-mybatis-plus-spring-boot-starter -am test -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 2: Install jfoundry updated artifacts locally**

Run:

```bash
mvn -Pmy-nexus -pl jfoundry-dependencies,jfoundry-starters,jfoundry-spring/jfoundry-spring-boot-starters/jfoundry-mybatis-plus-spring-boot-starter -am install -DskipTests -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Run DevCloud CI focused verification**

Run:

```bash
mvn -Pmy-nexus -pl ci-start -am test -Dtest='HelpDocument*Test,CiArchitectureBaselineTest' -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Commit jfoundry changes**

From `/Users/huangxiao/Workspace/mine/jfoundry`, run:

```bash
git diff --check
git add -A
git commit -m "refactor(starters): clarify layered starter boundaries"
```

- [ ] **Step 5: Commit DevCloud CI changes**

From `/Users/huangxiao/Workspace/mysoft/rdc_mks_aid/source-code/rdc/devcloud-ci-service`, stage only dependency updates:

```bash
git diff --check
git add ci-domain/pom.xml ci-app/pom.xml ci-adapter/pom.xml ci-infrastructure/pom.xml ci-start/pom.xml
git commit -m "refactor：调整 jfoundry starter 依赖边界"
```

Do not stage unrelated local files such as `.claude/`, `.gitignore`, or pending `docs/superpowers` files unless explicitly requested.
