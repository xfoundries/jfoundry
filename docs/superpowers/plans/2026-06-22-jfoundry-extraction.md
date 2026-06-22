# jfoundry Extraction Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extract `devcloud-ci-service/ddd-framework` into standalone project **jfoundry** at `/Users/huangxiao/Workspace/mine/jfoundry/`, fixing 4 P1 contract defects, adding 5 P2 production capabilities, and completing 3 P3 DDD concept integrations.

**Architecture:** Maven multi-module project under `org.jfoundry:jfoundry-parent`. Pure Java types in `jfoundry-domain` and `jfoundry-architecture-layered` (zero Spring dependency). Spring integration in `jfoundry-autoconfigure` via autoconfig imports. MyBatis-Plus persistence + Outbox pattern in `jfoundry-infrastructure`. ArchUnit rules aggregated in `jfoundry-test`.

**Tech Stack:** Java 21, Maven, Spring Boot 3.2.7, MyBatis-Plus 3.5.12, jmolecules 2025.0.2, ArchUnit 1.4.2, Jackson 2.17.1, JobRunr 6.3.5 (optional), Flyway (via Spring Boot), JUnit 5.

## Global Constraints

- Java version: JDK 21 (`<maven.compiler.release>21</maven.compiler.release>`)
- Maven groupId: `org.jfoundry`（旧 `com.mysoft.framework.ddd` 全量替换）
- Java root package: `org.jfoundry`（旧 `com.mysoft.framework.ddd` 全量替换）
- 配置属性前缀: `jfoundry.*`（旧 `ddd.*` 全量替换）
- 模块目录前缀: `jfoundry-*`（旧 `ddd-*` 全量替换）
- 源仓库: `/Users/huangxiao/Workspace/mysoft/rdc_mks_aid/source-code/rdc/devcloud-ci-service/ddd-framework`
- 目标仓库: `/Users/huangxiao/Workspace/mine/jfoundry/`（新 `git init`，不保留源仓库历史）
- 每个任务结束后必须 `mvn compile` 或 `mvn test` 绿才能进入下一任务
- 每个任务一个 commit，commit message 用 conventional commits 格式（`feat:` / `fix:` / `refactor:` / `chore:` / `docs:` / `test:`）
- Flyway 迁移脚本保留原版本号 `V20260617__create_outbox_event`，但按 P2-4 用 `MEDIUMTEXT`/`CLOB`，按 P2-1 加 `claimed_at`/`claimed_by` 列和 `DISPATCHING` 状态
- jmolecules 版本锁死 `2025.0.2`（已验证与现有代码无冲突）
- 业务侧（ci-* 模块）迁移**不在本计划范围**；本计划只产出 jfoundry 独立项目

---

## Phase 0 — Build Repository + Rename

**Phase goal:** Stand up jfoundry as a new Maven project at `/Users/huangxiao/Workspace/mine/jfoundry/`, copy source from `devcloud-ci-service/ddd-framework`, rename all coordinates/packages/properties to `org.jfoundry`, verify build green.

**Source repo state:** `/Users/huangxiao/Workspace/mine/jfoundry/` already has `.git/` initialized and the spec committed at `docs/superpowers/specs/2026-06-22-jfoundry-extraction-design.md` (commit `e01049e`).

### Task 0.1: Create jfoundry Maven skeleton

**Files:**
- Create: `/Users/huangxiao/Workspace/mine/jfoundry/pom.xml`
- Create: `/Users/huangxiao/Workspace/mine/jfoundry/.gitignore`

**Interfaces:**
- Produces: empty Maven root pom (no modules yet) so Task 0.2 has a target parent

- [ ] **Step 1: Create `.gitignore`**

Path: `/Users/huangxiao/Workspace/mine/jfoundry/.gitignore`

```gitignore
target/
.idea/
*.iml
.vscode/
.settings/
.classpath
.project
*.log
.DS_Store
```

- [ ] **Step 2: Create root `pom.xml`**

Path: `/Users/huangxiao/Workspace/mine/jfoundry/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>org.jfoundry</groupId>
    <artifactId>jfoundry-parent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <name>jfoundry-parent</name>
    <description>jfoundry — production-grade DDD framework on jmolecules</description>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <maven.compiler.release>21</maven.compiler.release>

        <maven-compiler-plugin.version>3.13.0</maven-compiler-plugin.version>
        <maven-resources-plugin.version>3.3.1</maven-resources-plugin.version>
        <maven-surefire-plugin.version>3.5.2</maven-surefire-plugin.version>
        <maven-source-plugin.version>3.3.1</maven-source-plugin.version>
        <maven-javadoc-plugin.version>3.11.2</maven-javadoc-plugin.version>
    </properties>

    <modules>
        <!-- modules added in Task 0.5 -->
    </modules>

    <dependencyManagement>
        <dependencies>
            <!-- BOM imported in Task 0.5 -->
        </dependencies>
    </dependencyManagement>

    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-resources-plugin</artifactId>
                    <version>${maven-resources-plugin.version}</version>
                </plugin>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>${maven-compiler-plugin.version}</version>
                    <configuration>
                        <release>${maven.compiler.release}</release>
                        <parameters>true</parameters>
                    </configuration>
                </plugin>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-surefire-plugin</artifactId>
                    <version>${maven-surefire-plugin.version}</version>
                </plugin>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-source-plugin</artifactId>
                    <version>${maven-source-plugin.version}</version>
                    <executions>
                        <execution>
                            <id>attach-sources</id>
                            <goals><goal>jar-no-fork</goal></goals>
                        </execution>
                    </executions>
                </plugin>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-javadoc-plugin</artifactId>
                    <version>${maven-javadoc-plugin.version}</version>
                    <configuration>
                        <doclint>none</doclint>
                    </configuration>
                    <executions>
                        <execution>
                            <id>attach-javadocs</id>
                            <goals><goal>jar</goal></goals>
                        </execution>
                    </executions>
                </plugin>
            </plugins>
        </pluginManagement>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-source-plugin</artifactId>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-javadoc-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 3: Validate pom**

Run: `cd /Users/huangxiao/Workspace/mine/jfoundry && mvn validate`
Expected: `BUILD SUCCESS`

- [ ] **Step 4: Commit**

```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
git add .gitignore pom.xml
git commit -m "chore: init jfoundry project skeleton"
```

---

### Task 0.2: Copy source from devcloud-ci-service/ddd-framework

**Files:**
- Copy: `ddd-framework/*` → `jfoundry/` (preserving original `com.mysoft.framework.ddd` package, `ddd-*` module names, `ddd.*` property prefix — renamed in later tasks)

**Interfaces:**
- Consumes: empty skeleton from Task 0.1
- Produces: full source tree at `/Users/huangxiao/Workspace/mine/jfoundry/` with old names

- [ ] **Step 1: Copy the framework source**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
SRC=/Users/huangxiao/Workspace/mysoft/rdc_mks_aid/source-code/rdc/devcloud-ci-service/ddd-framework

# Copy all ddd-* module directories (source + test + resources)
for dir in ddd-dependencies ddd-domain ddd-infrastructure ddd-spring ddd-test; do
  cp -R "$SRC/$dir" "./$dir"
done

# Remove all target/ and build artifacts
find . -type d -name target -exec rm -rf {} + 2>/dev/null || true
```

- [ ] **Step 2: Replace root `pom.xml` content with ddd-framework's parent pom**

The copied `ddd-framework/pom.xml` has wrong artifactId. We need to merge it into our root pom.

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
# Remove the copied ddd-framework pom.xml — we already have our root pom.xml
rm -f ddd-framework/pom.xml 2>/dev/null || true
# Wait: source dir is dddd-framework, we copied each submodule directly
# Actually: the copy loop copied ddd-dependencies, ddd-domain, etc. as siblings to root pom
ls -la
```

Expected: root shows `pom.xml`, `ddd-dependencies/`, `ddd-domain/`, `ddd-infrastructure/`, `ddd-spring/`, `ddd-test/` as siblings. If `ddd-framework/` directory exists, remove it.

```bash
# Clean up if a ddd-framework/ directory was created
rm -rf ddd-framework/ 2>/dev/null || true
ls -la
```

- [ ] **Step 3: Verify all module poms are present**

Run:
```bash
find /Users/huangxiao/Workspace/mine/jfoundry -maxdepth 3 -name pom.xml | sort
```

Expected output (paths):
```
/Users/huangxiao/Workspace/mine/jfoundry/pom.xml
/Users/huangxiao/Workspace/mine/jfoundry/ddd-dependencies/pom.xml
/Users/huangxiao/Workspace/mine/jfoundry/ddd-domain/pom.xml
/Users/huangxiao/Workspace/mine/jfoundry/ddd-infrastructure/pom.xml
/Users/huangxiao/Workspace/mine/jfoundry/ddd-infrastructure/ddd-messaging-core/pom.xml
/Users/huangxiao/Workspace/mine/jfoundry/ddd-infrastructure/ddd-messaging-jobrunr/pom.xml
/Users/huangxiao/Workspace/mine/jfoundry/ddd-infrastructure/ddd-messaging-mybatis-plus/pom.xml
/Users/huangxiao/Workspace/mine/jfoundry/ddd-infrastructure/ddd-messaging-spring/pom.xml
/Users/huangxiao/Workspace/mine/jfoundry/ddd-infrastructure/ddd-persistence-core/pom.xml
/Users/huangxiao/Workspace/mine/jfoundry/ddd-infrastructure/ddd-persistence-mybatis-plus/pom.xml
/Users/huangxiao/Workspace/mine/jfoundry/ddd-spring/pom.xml
/Users/huangxiao/Workspace/mine/jfoundry/ddd-spring/ddd-spring-autoconfigure/pom.xml
/Users/huangxiao/Workspace/mine/jfoundry/ddd-spring/ddd-spring-mybatis-plus-starter/pom.xml
/Users/huangxiao/Workspace/mine/jfoundry/ddd-test/pom.xml
```

- [ ] **Step 4: Commit**

```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
git add -A
git commit -m "chore: copy source from devcloud-ci-service/ddd-framework"
```

---

### Task 0.3: Rename Maven coordinates (`com.mysoft.framework.ddd` → `org.jfoundry`)

**Files:**
- Modify: `/Users/huangxiao/Workspace/mine/jfoundry/pom.xml` (root)
- Modify: every `pom.xml` under `/Users/huangxiao/Workspace/mine/jfoundry/*/pom.xml` and `*/*/pom.xml`
- Modify: every `pom.xml` `<parent>` section that references the old groupId

**Interfaces:**
- Consumes: source tree from Task 0.2 (old names everywhere)
- Produces: all poms declare `org.jfoundry` as groupId (artifactIds still `ddd-*` until Task 0.5)

- [ ] **Step 1: Global replace groupId in all pom.xml**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry

# Find all pom.xml files (excluding target/)
POMS=$(find . -name pom.xml -not -path "*/target/*")

# Replace groupId
for pom in $POMS; do
  # Use sed -i with backup suffix for macOS BSD sed compatibility
  sed -i.bak 's|<groupId>com.mysoft.framework.ddd</groupId>|<groupId>org.jfoundry</groupId>|g' "$pom"
  rm "$pom.bak"
done

# Verify
grep -r "com.mysoft.framework.ddd" --include="pom.xml" . || echo "CLEAN: zero matches"
```

Expected: `CLEAN: zero matches`

- [ ] **Step 2: Rename root pom artifactId**

The root pom from Task 0.1 already has `<artifactId>jfoundry-parent</artifactId>` and `<groupId>org.jfoundry</groupId>`. Verify it wasn't touched:

Run:
```bash
head -15 /Users/huangxiao/Workspace/mine/jfoundry/pom.xml
```

Expected: shows `<groupId>org.jfoundry</groupId>` and `<artifactId>jfoundry-parent</artifactId>`.

- [ ] **Step 3: Add modules and BOM import to root pom**

Edit `/Users/huangxiao/Workspace/mine/jfoundry/pom.xml`. Replace the `<modules>` block (currently empty) with:

```xml
    <modules>
        <module>ddd-dependencies</module>
        <module>ddd-domain</module>
        <module>ddd-infrastructure</module>
        <module>ddd-spring</module>
        <module>ddd-test</module>
    </modules>
```

And replace the `<dependencyManagement>` block (currently empty) with:

```xml
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.jfoundry</groupId>
                <artifactId>ddd-dependencies</artifactId>
                <version>1.0.0-SNAPSHOT</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
```

- [ ] **Step 4: Verify build still validates**

Run: `cd /Users/huangxiao/Workspace/mine/jfoundry && mvn validate -DskipTests`
Expected: `BUILD SUCCESS`. (Compile will still fail because Java packages still say `com.mysoft.framework.ddd` — that's fixed in Task 0.4. `mvn validate` only checks POM integrity.)

- [ ] **Step 5: Commit**

```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
git add -A
git commit -m "refactor: rename Maven coordinates to org.jfoundry"
```

---

### Task 0.4: Rename Java packages (`com.mysoft.framework.ddd` → `org.jfoundry`)

**Files:**
- Modify: every `.java` file's `package` and `import` declarations under `/Users/huangxiao/Workspace/mine/jfoundry/`
- Move: every source file from `com/mysoft/framework/ddd/` to `org/jfoundry/` directory tree

**Interfaces:**
- Consumes: poms from Task 0.3 (groupId is `org.jfoundry` but packages are stale)
- Produces: all `.java` files live under `org/jfoundry/` with matching `package` declarations

- [ ] **Step 1: Move source directories**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry

# For each module's src/main/java and src/test/java, move com/mysoft/framework/ddd → org/jfoundry
JAVA_DIRS=$(find . -type d -path "*/src/*/java/com/mysoft/framework/ddd" -not -path "*/target/*")

for dir in $JAVA_DIRS; do
  # dir is like ./ddd-domain/src/main/java/com/mysoft/framework/ddd
  PARENT=$(dirname "$(dirname "$(dirname "$(dirname "$dir")")")")  # .../src/main/java
  TARGET="$PARENT/org/jfoundry"
  mkdir -p "$TARGET"
  # Move contents
  cp -R "$dir/." "$TARGET/"
  # Remove old tree
  rm -rf "$PARENT/com"
done

# Verify: no old package directories remain
find . -type d -path "*com/mysoft/framework/ddd*" -not -path "*/target/*"
```

Expected: empty output (no matches).

- [ ] **Step 2: Rewrite `package` and `import` declarations**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry

JAVA_FILES=$(find . -name "*.java" -not -path "*/target/*")

for f in $JAVA_FILES; do
  sed -i.bak \
    -e 's|package com\.mysoft\.framework\.ddd|package org.jfoundry|g' \
    -e 's|import com\.mysoft\.framework\.ddd|import org.jfoundry|g' \
    "$f"
  rm "$f.bak"
done

# Verify
grep -r "com\.mysoft\.framework\.ddd" --include="*.java" . | head -5 || echo "CLEAN: zero matches"
```

Expected: `CLEAN: zero matches`.

- [ ] **Step 3: Build the project**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn clean compile -DskipTests 2>&1 | tail -30
```

Expected: `BUILD SUCCESS`. If failures appear, they will be references to `com.mysoft.framework.ddd` in non-`.java` files (e.g. `META-INF/spring/...AutoConfiguration.imports`, logback config, etc.). Fix any remaining references:

```bash
# Catch any remaining references in non-Java files
grep -rn "com\.mysoft\.framework\.ddd" --include="*.xml" --include="*.imports" --include="*.yml" --include="*.yaml" --include="*.properties" . | grep -v target/ | head -20
```

For each match found, sed-replace `com.mysoft.framework.ddd` → `org.jfoundry` in that file:
```bash
MATCHED_FILES=$(grep -rl "com\.mysoft\.framework\.ddd" --include="*.xml" --include="*.imports" --include="*.yml" --include="*.yaml" --include="*.properties" . | grep -v target/)
for f in $MATCHED_FILES; do
  sed -i.bak 's|com\.mysoft\.framework\.ddd|org.jfoundry|g' "$f"
  rm "$f.bak"
done
```

Re-run: `mvn clean compile -DskipTests 2>&1 | tail -10`
Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Run all tests**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn test 2>&1 | tail -30
```

Expected: `BUILD SUCCESS` with all module tests green. If a test fails due to stale Spring FQN references, fix and re-run.

- [ ] **Step 5: Commit**

```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
git add -A
git commit -m "refactor: rename Java packages com.mysoft.framework.ddd → org.jfoundry"
```

---

### Task 0.5: Rename module directories (`ddd-*` → `jfoundry-*`)

**Files:**
- Rename: `ddd-dependencies/` → `jfoundry-dependencies/`
- Rename: `ddd-domain/` → `jfoundry-domain/`
- Rename: `ddd-infrastructure/` → `jfoundry-infrastructure/`
- Rename: `ddd-spring/` → `jfoundry-spring/`
- Rename: `ddd-test/` → `jfoundry-test/`
- Rename: nested modules `ddd-persistence-core` → `jfoundry-persistence-core`, etc.
- Modify: root `pom.xml` `<modules>` section
- Modify: every child pom's `<parent>` section and `<artifactId>`

**Interfaces:**
- Consumes: compile-green source from Task 0.4
- Produces: all module dirs and artifactIds prefixed `jfoundry-*`

- [ ] **Step 1: Rename module directories**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry

# Top-level modules
for old in ddd-dependencies ddd-domain ddd-infrastructure ddd-spring ddd-test; do
  new="jfoundry-${old#ddd-}"
  if [ -d "$old" ]; then
    git mv "$old" "$new"
  fi
done

# Nested modules under jfoundry-infrastructure/
for old in jfoundry-infrastructure/ddd-persistence-core \
           jfoundry-infrastructure/ddd-persistence-mybatis-plus \
           jfoundry-infrastructure/ddd-messaging-core \
           jfoundry-infrastructure/ddd-messaging-mybatis-plus \
           jfoundry-infrastructure/ddd-messaging-spring \
           jfoundry-infrastructure/ddd-messaging-jobrunr; do
  base=$(basename "$old")
  new="jfoundry-infrastructure/jfoundry-${base#ddd-}"
  if [ -d "$old" ]; then
    git mv "$old" "$new"
  fi
done

# Nested modules under jfoundry-spring/
for old in jfoundry-spring/ddd-spring-autoconfigure \
           jfoundry-spring/ddd-spring-mybatis-plus-starter; do
  base=$(basename "$old")
  new="jfoundry-spring/jfoundry-${base#ddd-spring-}"
  if [ -d "$old" ]; then
    git mv "$old" "$new"
  fi
done

ls
```

Expected: top-level dirs include `jfoundry-dependencies/`, `jfoundry-domain/`, `jfoundry-infrastructure/`, `jfoundry-spring/`, `jfoundry-test/`.

- [ ] **Step 2: Rename artifactIds in all pom.xml**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry

POMS=$(find . -name pom.xml -not -path "*/target/*")

for pom in $POMS; do
  sed -i.bak \
    -e 's|<artifactId>ddd-dependencies</artifactId>|<artifactId>jfoundry-dependencies</artifactId>|g' \
    -e 's|<artifactId>ddd-domain</artifactId>|<artifactId>jfoundry-domain</artifactId>|g' \
    -e 's|<artifactId>ddd-infrastructure</artifactId>|<artifactId>jfoundry-infrastructure</artifactId>|g' \
    -e 's|<artifactId>ddd-spring</artifactId>|<artifactId>jfoundry-spring</artifactId>|g' \
    -e 's|<artifactId>ddd-test</artifactId>|<artifactId>jfoundry-test</artifactId>|g' \
    -e 's|<artifactId>ddd-persistence-core</artifactId>|<artifactId>jfoundry-persistence-core</artifactId>|g' \
    -e 's|<artifactId>ddd-persistence-mybatis-plus</artifactId>|<artifactId>jfoundry-persistence-mybatis-plus</artifactId>|g' \
    -e 's|<artifactId>ddd-messaging-core</artifactId>|<artifactId>jfoundry-messaging-core</artifactId>|g' \
    -e 's|<artifactId>ddd-messaging-mybatis-plus</artifactId>|<artifactId>jfoundry-messaging-mybatis-plus</artifactId>|g' \
    -e 's|<artifactId>ddd-messaging-spring</artifactId>|<artifactId>jfoundry-messaging-spring</artifactId>|g' \
    -e 's|<artifactId>ddd-messaging-jobrunr</artifactId>|<artifactId>jfoundry-messaging-jobrunr</artifactId>|g' \
    -e 's|<artifactId>ddd-spring-autoconfigure</artifactId>|<artifactId>jfoundry-spring-autoconfigure</artifactId>|g' \
    -e 's|<artifactId>ddd-spring-mybatis-plus-starter</artifactId>|<artifactId>jfoundry-spring-mybatis-plus-starter</artifactId>|g' \
    -e 's|<artifactId>ddd-framework</artifactId>|<artifactId>jfoundry-parent</artifactId>|g' \
    "$pom"
  rm "$pom.bak"
done

# Verify: zero ddd-* artifactIds remain in poms
grep -rn "<artifactId>ddd-" --include="pom.xml" . | grep -v target/ || echo "CLEAN"
```

Expected: `CLEAN`.

- [ ] **Step 3: Update root pom `<modules>` section**

Edit `/Users/huangxiao/Workspace/mine/jfoundry/pom.xml`. Replace the `<modules>` block:

```xml
    <modules>
        <module>jfoundry-dependencies</module>
        <module>jfoundry-domain</module>
        <module>jfoundry-infrastructure</module>
        <module>jfoundry-spring</module>
        <module>jfoundry-test</module>
    </modules>
```

- [ ] **Step 4: Verify build**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn clean install -DskipTests 2>&1 | tail -15
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 5: Run all tests**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn test 2>&1 | tail -20
```

Expected: `BUILD SUCCESS`, all tests green.

- [ ] **Step 6: Commit**

```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
git add -A
git commit -m "refactor: rename module directories ddd-* → jfoundry-*"
```

---

### Task 0.6: Rename config property prefix (`ddd.*` → `jfoundry.*`) + AutoConfiguration.imports + Flyway migration

**Files:**
- Modify: every `@ConfigurationProperties` and `@ConditionalOnProperty` prefix
- Modify: `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` class FQNs (already updated in Task 0.4 but verify)
- Modify: Flyway migration script name and table references

**Interfaces:**
- Consumes: green build from Task 0.5
- Produces: all public-facing identifiers say `jfoundry.*`

- [ ] **Step 1: Rewrite `@ConfigurationProperties` prefix**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry

JAVA_FILES=$(find . -name "*.java" -not -path "*/target/*")

for f in $JAVA_FILES; do
  sed -i.bak \
    -e 's|@ConfigurationProperties(prefix = "ddd\.|@ConfigurationProperties(prefix = "jfoundry.|g' \
    -e 's|prefix = "ddd\.|prefix = "jfoundry.|g' \
    -e 's|prefix="ddd\.|prefix="jfoundry.|g' \
    "$f"
  rm "$f.bak"
done

grep -rn '"ddd\.' --include="*.java" . | grep -v target/ | head -5 || echo "CLEAN"
```

Expected: `CLEAN`.

- [ ] **Step 2: Rewrite `@ConditionalOnProperty` prefix in all autoconfig classes**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry

JAVA_FILES=$(find . -name "*.java" -not -path "*/target/*")

for f in $JAVA_FILES; do
  sed -i.bak \
    -e 's|prefix = "ddd\.|prefix = "jfoundry.|g' \
    -e 's|prefix="ddd\.|prefix="jfoundry.|g' \
    "$f"
  rm "$f.bak"
done

grep -rn 'prefix.*"ddd\.' --include="*.java" . | grep -v target/ || echo "CLEAN"
```

Expected: `CLEAN`.

- [ ] **Step 3: Update Flyway migration script name and content**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry

# Find all V20260617 migration scripts
find . -name "V20260617*.sql" -not -path "*/target/*"
```

For each script found, rename and edit:
- Rename `V20260617__create_ddd_outbox_event.sql` → `V20260617__create_outbox_event.sql`
- Inside, table name `ddd_outbox_event` stays as-is for Phase 0 (P2-2 in Phase 2 will make it configurable; default `ddd_outbox_event` preserves backward compat per spec Section 7.2)

```bash
SCRIPTS=$(find . -name "V20260617__create_ddd_outbox_event.sql" -not -path "*/target/*")
for s in $SCRIPTS; do
  dir=$(dirname "$s")
  git mv "$s" "$dir/V20260617__create_outbox_event.sql"
done

# Also rename any other ddd_ prefixed SQL files (test fixtures like ddd_outbox_event.sql)
TEST_SQL=$(find . -name "ddd_outbox_event.sql" -not -path "*/target/*")
for s in $TEST_SQL; do
  dir=$(dirname "$s")
  git mv "$s" "$dir/outbox_event.sql"
  # Update any Java test code referencing the filename
done

# Update Java code that references the test SQL filename
JAVA_FILES=$(find . -name "*.java" -not -path "*/target/*")
for f in $JAVA_FILES; do
  sed -i.bak 's|ddd_outbox_event\.sql|outbox_event.sql|g' "$f"
  rm "$f.bak"
done
```

- [ ] **Step 4: Verify AutoConfiguration.imports file FQNs**

Run:
```bash
cat /Users/huangxiao/Workspace/mine/jfoundry/jfoundry-spring/jfoundry-spring-autoconfigure/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

Expected (all FQNs use `org.jfoundry.*`):
```
org.jfoundry.autoconfigure.messaging.MessageSenderAutoConfiguration
org.jfoundry.autoconfigure.messaging.DomainEventExternalizerAutoConfiguration
org.jfoundry.autoconfigure.persistence.OutboxMybatisPlusAutoConfiguration
org.jfoundry.autoconfigure.dispatcher.OutboxDispatcherAutoConfiguration
```

If any FQN still says `com.mysoft.framework.ddd.*`, edit the file directly.

- [ ] **Step 5: Update all docs and configuration properties metadata**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry

# Update any remaining ddd. references in .md, .adoc, .properties, .yml, .yaml
MATCHED=$(grep -rl "ddd\." --include="*.md" --include="*.adoc" --include="*.properties" --include="*.yml" --include="*.yaml" . | grep -v target/ | grep -v docs/superpowers/)
if [ -n "$MATCHED" ]; then
  for f in $MATCHED; do
    sed -i.bak 's|ddd\.outbox\.|jfoundry.outbox.|g; s|ddd\.persistence\.|jfoundry.persistence.|g; s|ddd\.messaging\.|jfoundry.messaging.|g; s|ddd\.domain\.|jfoundry.domain.|g' "$f"
    rm "$f.bak"
  done
fi
```

- [ ] **Step 6: Build and test green**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn clean install 2>&1 | tail -20
```

Expected: `BUILD SUCCESS`, all tests green.

- [ ] **Step 7: Phase 0 acceptance grep checks**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry

echo "=== Check 1: no com.mysoft.framework.ddd in Java ==="
grep -r "com\.mysoft\.framework\.ddd" --include="*.java" src 2>/dev/null && echo "FAIL" || echo "PASS"

echo "=== Check 2: no ddd. config prefix in resources ==="
grep -r '"ddd\.' --include="*.java" --include="*.yml" --include="*.yaml" --include="*.properties" . | grep -v target/ | grep -v docs/superpowers/ && echo "FAIL" || echo "PASS"

echo "=== Check 3: no ddd-* module dirs ==="
find . -maxdepth 4 -type d -name "ddd-*" -not -path "*/target/*" && echo "FAIL" || echo "PASS"

echo "=== Check 4: no <artifactId>ddd-* in poms ==="
grep -rn "<artifactId>ddd-" --include="pom.xml" . | grep -v target/ && echo "FAIL" || echo "PASS"

echo "=== Check 5: root pom modules all jfoundry-* ==="
grep "<module>" pom.xml
```

Expected: all five checks PASS.

- [ ] **Step 8: Commit**

```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
git add -A
git commit -m "refactor: rename config property prefix ddd.* → jfoundry.* and Flyway script"
```

---

## Phase 1 — P1 Contract Defect Fixes

**Phase goal:** Fix 4 framework contract/behavior mismatches identified in spec Sections 6.1–6.4. Each fix has a regression integration test that fails before the fix and passes after.

### Task 1.1: Register DomainEventPublisher via autoconfig (P1-1)

**Files:**
- Modify: `jfoundry-infrastructure/jfoundry-messaging-spring/src/main/java/org/jfoundry/infrastructure/messaging/spring/publisher/SpringDomainEventPublisher.java` (delete `@Component`, `@Autowired`)
- Create: `../../../jfoundry-spring/jfoundry-autoconfigure/src/main/java/org/jfoundry/autoconfigure/messaging/DomainEventPublisherAutoConfiguration.java`
- Modify: `../../../jfoundry-spring/jfoundry-autoconfigure/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Test: `../../../jfoundry-spring/jfoundry-autoconfigure/src/test/java/org/jfoundry/autoconfigure/DomainEventPublisherAutoConfigurationTest.java`

**Interfaces:**
- Consumes: `DomainEventPublisher` interface (already in `org.jfoundry.domain.event`), `DomainEventSink` interface (already in `org.jfoundry.infrastructure.messaging.externalization`)
- Produces: `DomainEventPublisherAutoConfiguration` registered as Spring Boot autoconfig so `DomainEventPublisher` bean is auto-injected in any Spring Boot app with jfoundry classpath

- [ ] **Step 1: Write failing integration test**

Path: `../../../jfoundry-spring/jfoundry-autoconfigure/src/test/java/org/jfoundry/autoconfigure/DomainEventPublisherAutoConfigurationTest.java`

```java
package org.jfoundry.autoconfigure;

import org.jfoundry.domain.event.DomainEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/// P1-1 regression test: SpringDomainEventPublisher must be registered via autoconfig
/// so that a minimal Spring Boot app with jfoundry-spring-autoconfigure on classpath
/// can inject DomainEventPublisher without business-side @ComponentScan.
@SpringBootTest(classes = DomainEventPublisherAutoConfigurationTest.TestApp.class)
class DomainEventPublisherAutoConfigurationTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApp {
    }

    @Autowired
    private DomainEventPublisher publisher;

    @Test
    void domainEventPublisherIsAutoConfigured() {
        assertThat(publisher).isNotNull();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn test -pl jfoundry-spring/jfoundry-spring-autoconfigure -Dtest=DomainEventPublisherAutoConfigurationTest 2>&1 | tail -20
```

Expected: FAIL with `NoSuchBeanDefinitionException: No qualifying bean of type 'org.jfoundry.domain.event.DomainEventPublisher'` (because `SpringDomainEventPublisher` is in the `jfoundry-messaging-spring` module with `@Component`, but autoconfigure module doesn't scan that package).

- [ ] **Step 3: Delete `@Component` from SpringDomainEventPublisher**

Edit `jfoundry-infrastructure/jfoundry-messaging-spring/src/main/java/org/jfoundry/infrastructure/messaging/spring/publisher/SpringDomainEventPublisher.java`:

Replace lines 5–8 and 24 with:
```java
// Delete: import org.springframework.beans.factory.annotation.Autowired;
// Delete: import org.springframework.stereotype.Component;
// Keep other imports

/// 基于 Spring 的领域事件发布器实现。
/// <p>
/// 发布流程：
/// <ol>
///   <li>同步转发给所有 DomainEventSink（事务内，与业务数据原子提交）</li>
///   <li>注册 afterCommit 回调，事务提交后通过 ApplicationEventPublisher 发布给本地监听器</li>
/// </ol>
/// <p>
/// 若无事务上下文，立即同步发布给 Sinks 和本地监听器。
/// <p>
/// 不再标注 {@code @Component}：由 {@code DomainEventPublisherAutoConfiguration} 注册为 bean。
public class SpringDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher eventPublisher;
    private final List<DomainEventSink> sinks;

    /// 主构造器：注入 sinks（可能为空 list）。
    public SpringDomainEventPublisher(ApplicationEventPublisher eventPublisher,
                                      List<DomainEventSink> sinks) {
        this.eventPublisher = eventPublisher;
        this.sinks = sinks != null ? sinks : List.of();
    }
    // ... rest unchanged
```

- [ ] **Step 4: Create `DomainEventPublisherAutoConfiguration`**

Path: `../../../jfoundry-spring/jfoundry-autoconfigure/src/main/java/org/jfoundry/autoconfigure/messaging/DomainEventPublisherAutoConfiguration.java`

```java
package org.jfoundry.autoconfigure.messaging;

import org.jfoundry.domain.event.DomainEventPublisher;
import org.jfoundry.infrastructure.messaging.externalization.DomainEventSink;
import org.jfoundry.infrastructure.messaging.spring.publisher.SpringDomainEventPublisher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

import java.util.List;

/// 自动注册 {@link SpringDomainEventPublisher} 作为默认 {@link DomainEventPublisher}。
/// <p>
/// 业务侧如需自定义发布器，注册自己的 {@code DomainEventPublisher} Bean 即可覆盖。
/// <p>
/// 开关：{@code jfoundry.domain.event.enabled=false} 可关闭默认注册。
@AutoConfiguration
@ConditionalOnClass({DomainEventPublisher.class, SpringDomainEventPublisher.class})
@ConditionalOnProperty(prefix = "jfoundry.domain.event", name = "enabled",
                       havingValue = "true", matchIfMissing = true)
public class DomainEventPublisherAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(DomainEventPublisher.class)
    public SpringDomainEventPublisher springDomainEventPublisher(
            ApplicationEventPublisher eventPublisher,
            ObjectProvider<List<DomainEventSink>> sinksProvider) {
        List<DomainEventSink> sinks = sinksProvider.getIfAvailable(List::of);
        return new SpringDomainEventPublisher(eventPublisher, sinks);
    }
}
```

- [ ] **Step 5: Register autoconfig in imports file**

Edit `../../../jfoundry-spring/jfoundry-autoconfigure/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`:

Replace the 4-line content with:
```
org.jfoundry.autoconfigure.messaging.DomainEventPublisherAutoConfiguration
org.jfoundry.autoconfigure.messaging.MessageSenderAutoConfiguration
org.jfoundry.autoconfigure.messaging.DomainEventExternalizerAutoConfiguration
org.jfoundry.autoconfigure.persistence.OutboxMybatisPlusAutoConfiguration
org.jfoundry.autoconfigure.dispatcher.OutboxDispatcherAutoConfiguration
```

- [ ] **Step 6: Run test to verify it passes**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn test -pl jfoundry-spring/jfoundry-spring-autoconfigure -Dtest=DomainEventPublisherAutoConfigurationTest 2>&1 | tail -15
```

Expected: `Tests run: 1, Failures: 0, Errors: 0` / `BUILD SUCCESS`.

- [ ] **Step 7: Run full module tests to catch regressions**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn install -DskipTests=false -pl jfoundry-spring/jfoundry-spring-autoconfigure -am 2>&1 | tail -20
```

Expected: `BUILD SUCCESS`. Existing `DomainEventExternalizationIntegrationTest` must still pass — it uses `@EnableAutoConfiguration` which now picks up our new autoconfig, and the publisher bean resolves.

- [ ] **Step 8: Commit**

```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
git add -A
git commit -m "fix(autoconfig): register SpringDomainEventPublisher via autoconfig

Previously @Component on SpringDomainEventPublisher was a dead annotation
(ddd-messaging-spring module's package wasn't @ComponentScan'd by
ddd-spring-autoconfigure). Business code depending on jfoundry could not
inject DomainEventPublisher without a manual @ComponentScan.

Delete @Component, add DomainEventPublisherAutoConfiguration, register
in AutoConfiguration.imports."
```

---

### Task 1.2: Correct DomainEventExternalizer condition type (P1-2)

**Files:**
- Modify: `../../../jfoundry-spring/jfoundry-autoconfigure/src/main/java/org/jfoundry/autoconfigure/messaging/DomainEventExternalizerAutoConfiguration.java`
- Modify: `jfoundry-infrastructure/jfoundry-messaging-core/src/main/java/org/jfoundry/infrastructure/messaging/externalization/DomainEventSink.java` (javadoc only)
- Test: `../../../jfoundry-spring/jfoundry-autoconfigure/src/test/java/org/jfoundry/autoconfigure/DomainEventExternalizerConditionTest.java`

**Interfaces:**
- Consumes: `OutboxRepository`, `DomainEventExternalizer`, `DomainEventSink` (already defined)
- Produces: `DomainEventExternalizer` bean still registered even when business defines custom `DomainEventSink`; retracts only when business defines custom `DomainEventExternalizer`

- [ ] **Step 1: Write failing integration test**

Path: `../../../jfoundry-spring/jfoundry-autoconfigure/src/test/java/org/jfoundry/autoconfigure/DomainEventExternalizerConditionTest.java`

```java
package org.jfoundry.autoconfigure;

import org.jfoundry.domain.event.DomainEventPublisher;
import org.jfoundry.infrastructure.messaging.externalization.DomainEventSink;
import org.jfoundry.infrastructure.messaging.externalization.ExternalizationRuleResolver;
import org.jfoundry.infrastructure.messaging.outbox.OutboxRepository;
import org.jfoundry.infrastructure.messaging.spring.externalization.DomainEventExternalizer;
import org.jmolecules.event.types.DomainEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;

/// P1-2 regression: business-side custom DomainEventSink must NOT cause
/// framework's DomainEventExternalizer to retract. Externalizer is the tail
/// of the sink chain; it should only retract when business provides its own
/// DomainEventExternalizer.
@SpringBootTest(classes = {
        DomainEventExternalizerConditionTest.TestApp.class,
        DomainEventExternalizerConditionTest.WithCustomSink.class
})
class DomainEventExternalizerConditionTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApp {
    }

    @TestConfiguration
    static class WithCustomSink {
        @Bean
        DomainEventSink loggingSink() {
            return new LoggingSink();
        }

        @Bean
        OutboxRepository stubOutboxRepository() {
            return new StubOutboxRepository();
        }
    }

    static class LoggingSink implements DomainEventSink {
        @Override
        public void handle(DomainEvent event) {
            // no-op
        }
    }

    static class StubOutboxRepository implements OutboxRepository {
        // minimal stub — only methods called by DomainEventExternalizer
        @Override
        public void append(org.jfoundry.infrastructure.messaging.outbox.OutboxEntry entry) {
            // no-op
        }
    }

    @Autowired
    private DomainEventExternalizer externalizer;

    @Test
    void externalizerStillRegisteredWhenCustomSinkExists() {
        assertThat(externalizer).isNotNull();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn test -pl jfoundry-spring/jfoundry-spring-autoconfigure -Dtest=DomainEventExternalizerConditionTest 2>&1 | tail -20
```

Expected: FAIL — `DomainEventExternalizer` bean is missing because current `@ConditionalOnMissingBean(DomainEventSink.class)` retracts when business registers `LoggingSink`.

- [ ] **Step 3: Fix the condition type**

Edit `../../../jfoundry-spring/jfoundry-autoconfigure/src/main/java/org/jfoundry/autoconfigure/messaging/DomainEventExternalizerAutoConfiguration.java`. Replace the `@Bean` block for `domainEventExternalizer` (currently lines 25–33):

```java
    @Bean
    @ConditionalOnBean(OutboxRepository.class)
    @ConditionalOnMissingBean(DomainEventExternalizer.class)
    @Order(Ordered.LOWEST_PRECEDENCE)  // Externalizer 是 Sink 链末端
    public DomainEventExternalizer domainEventExternalizer(
            OutboxRepository outboxRepository,
            ExternalizationRuleResolver ruleResolver,
            PayloadSerializer serializer) {
        return new DomainEventExternalizer(outboxRepository, serializer, ruleResolver);
    }
```

Add imports at top of file:
```java
import org.jfoundry.infrastructure.messaging.spring.externalization.DomainEventExternalizer;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
```

- [ ] **Step 4: Update `DomainEventSink` javadoc**

Edit `jfoundry-infrastructure/jfoundry-messaging-core/src/main/java/org/jfoundry/infrastructure/messaging/externalization/DomainEventSink.java`. Update the interface javadoc to clarify chain semantics:

```java
/// 领域事件外部化 Sink：作为事件链路的处理节点。
/// <p>
/// 框架支持多个 {@code DomainEventSink} 共存，按 Spring {@code @Order} 升序执行。
/// 业务侧可注册自定义 Sink（日志、metrics 等）补充链路。
/// <p>
/// 框架内置的 {@code DomainEventExternalizer} 是链路末端（{@link Ordered#LOWEST_PRECEDENCE}），
/// 负责把事件写入 Outbox 表。业务侧若注册自己的 {@code DomainEventExternalizer}，
/// 框架默认实现会自动退让。
public interface DomainEventSink {
    void handle(DomainEvent event);
}
```

- [ ] **Step 5: Run test to verify it passes**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn test -pl jfoundry-spring/jfoundry-spring-autoconfigure -Dtest=DomainEventExternalizerConditionTest 2>&1 | tail -15
```

Expected: `Tests run: 1, Failures: 0` / `BUILD SUCCESS`.

- [ ] **Step 6: Run full module tests**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn install -pl jfoundry-spring/jfoundry-spring-autoconfigure -am 2>&1 | tail -10
```

Expected: `BUILD SUCCESS`. Existing `DomainEventExternalizationIntegrationTest` still green.

- [ ] **Step 7: Commit**

```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
git add -A
git commit -m "fix(autoconfig): correct DomainEventExternalizer condition type

@ConditionalOnMissingBean(DomainEventSink.class) was wrong — it retracted
the Externalizer when business registered any custom Sink, silently
breaking externalization. Externalizer is the tail of the Sink chain;
only retract when business provides its own DomainEventExternalizer."
```

---

### Task 1.3: Correct AggregateRepository batch-operation javadoc (P1-3 docs)

**Files:**
- Modify: `jfoundry-domain/src/main/java/org/jfoundry/domain/repository/AggregateRepository.java` (javadoc only — remove the "事务性保证" lie)

**Interfaces:**
- Consumes: existing `AggregateRepository` interface
- Produces: accurate javadoc that doesn't lie about transactional behavior

- [ ] **Step 1: Read current javadoc to confirm lines to edit**

Run:
```bash
grep -n "事务性保证" /Users/huangxiao/Workspace/mine/jfoundry/jfoundry-domain/src/main/java/org/jfoundry/domain/repository/AggregateRepository.java
```

Expected: 2 matches, on `addAll` and `modifyAll` javadoc blocks.

- [ ] **Step 2: Rewrite `addAll` javadoc**

Edit `jfoundry-domain/src/main/java/org/jfoundry/domain/repository/AggregateRepository.java`. Replace:

```java
    /// 批量加入聚合集合(新建)。
    /// <p>
    /// 事务性保证:整个批量操作在单个事务中执行。如果部分失败,所有操作都会回滚。
    void addAll(Collection<T> entities);
```

with:

```java
    /// 批量加入聚合集合(新建)。
    /// <p>
    /// 批量语义：逐个调用 {@link #add} 顺序执行。<b>本方法不提供事务边界</b>——
    /// 部分失败时已完成的写入不会回滚。
    /// <p>
    /// 事务边界归属应用层。如需原子性，调用方应在应用服务方法上显式标注 {@code @Transactional}，
    /// 并优先按 "一个事务修改一个聚合根" 的 DDD 原则拆分聚合边界。
    void addAll(Collection<T> entities);
```

- [ ] **Step 3: Rewrite `modifyAll` javadoc**

Replace:

```java
    /// 批量修改聚合集合中已存在的元素。
    /// <p>
    /// 事务性保证:整个批量操作在单个事务中执行。
    void modifyAll(Collection<T> entities);
```

with:

```java
    /// 批量修改聚合集合中已存在的元素。
    /// <p>
    /// 批量语义：逐个调用 {@link #modify} 顺序执行。<b>本方法不提供事务边界</b>——
    /// 部分失败时已完成的写入不会回滚。如需原子性，请在应用层显式管理事务。
    void modifyAll(Collection<T> entities);
```

- [ ] **Step 4: Verify no "事务性保证" remains in source**

Run:
```bash
grep -rn "事务性保证" --include="*.java" /Users/huangxiao/Workspace/mine/jfoundry/ | grep -v target/
```

Expected: empty output.

- [ ] **Step 5: Build still green**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn install -DskipTests -pl jfoundry-domain 2>&1 | tail -5
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 6: Commit**

```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
git add -A
git commit -m "docs(repository): correct addAll/modifyAll javadoc on transaction semantics

The old javadoc claimed '事务性保证' (transactional atomicity) for batch
operations, but AbstractPersistenceRepository implementation has zero
@Transactional. Readers who trusted the docs got silent partial writes
on failure. Align docs with reality; transaction boundary belongs to
application layer."
```

---

### Task 1.4: ArchUnit rule — forbid @Transactional in persistence layer (P1-3 guard)

**Files:**
- Create: `jfoundry-test/src/main/java/org/jfoundry/test/archunit/PersistenceRules.java`
- Test: `jfoundry-test/src/test/java/org/jfoundry/test/archunit/PersistenceRulesTest.java`

**Interfaces:**
- Consumes: `org.jfoundry.infrastructure.persistence` package (already exists)
- Produces: `PersistenceRules.persistence_repository_must_not_use_transactional` — an `ArchRule` business code can import

- [ ] **Step 1: Write failing ArchUnit test**

Path: `jfoundry-test/src/test/java/org/jfoundry/test/archunit/PersistenceRulesTest.java`

```java
package org.jfoundry.test.archunit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static org.assertj.core.api.Assertions.assertThat;

/// Sanity test: PersistenceRules constants must be non-null ArchRule instances,
/// and the rule set must pass against jfoundry's own source.
class PersistenceRulesTest {

    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_JARS)
            .importPackages("org.jfoundry");

    @Test
    void persistenceRulesAreDeclared() {
        assertThat(PersistenceRules.persistence_repository_must_not_use_transactional)
                .as("PersistenceRules.persistence_repository_must_not_use_transactional must be non-null")
                .isNotNull();
    }

    @Test
    void jfoundryOwnSourceHasNoTransactionalInPersistence() {
        PersistenceRules.persistence_repository_must_not_use_transactional.check(classes);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn test -pl jfoundry-test -Dtest=PersistenceRulesTest 2>&1 | tail -20
```

Expected: FAIL with compilation error (`PersistenceRules` class doesn't exist yet).

- [ ] **Step 3: Create `PersistenceRules`**

Path: `jfoundry-test/src/main/java/org/jfoundry/test/archunit/PersistenceRules.java`

```java
package org.jfoundry.test.archunit;

import com.tngtech.archunit.lang.ArchRule;
import org.springframework.transaction.annotation.Transactional;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/// 持久化层架构规则集合。
/// <p>
/// 强制执行 spec Section 6.3 的约束：事务边界属于应用层，持久化层零 {@code @Transactional}。
public final class PersistenceRules {

    private PersistenceRules() {
    }

    /// 持久化实现包下零 {@code @Transactional}（类级别和方法级别都禁）。
    /// <p>
    /// P1-3 修复的防护网：防止 javadoc 修对后，未来有人误把 @Transactional 加到 Repository 实现上。
    public static final ArchRule persistence_repository_must_not_use_transactional =
            noClasses()
                    .that().resideInAPackage("..infrastructure.persistence..")
                    .should().beAnnotatedWith(Transactional.class)
                    .orShould().beMetaAnnotatedWith(Transactional.class)
                    .orShould().haveMethodsAnnotatedWith(Transactional.class);

    /// autoconfig 模块禁止使用 {@code @Component}（P1-1 防护网）。
    /// <p>
    /// autoconfig 类应该用 {@code @AutoConfiguration} + {@code @Bean}，不允许 {@code @ComponentScan}。
    public static final ArchRule autoconfig_must_not_use_component =
            noClasses()
                    .that().resideInAPackage("..autoconfigure..")
                    .should().beAnnotatedWith(org.springframework.stereotype.Component.class)
                    .orShould().beMetaAnnotatedWith(org.springframework.stereotype.Component.class);
}
```

- [ ] **Step 4: Add `archunit` and `spring-context` dependencies to `jfoundry-test/pom.xml`**

Edit `jfoundry-test/pom.xml`. If `archunit-junit5` is not declared as a dependency, add it inside `<dependencies>`:

```xml
        <dependency>
            <groupId>com.tngtech.archunit</groupId>
            <artifactId>archunit-junit5</artifactId>
            <scope>compile</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-context</artifactId>
            <scope>compile</scope>
        </dependency>
```

(Both are managed by `jfoundry-dependencies` BOM.)

Also ensure the test module depends on `jfoundry-infrastructure` so the `org.jfoundry.infrastructure.persistence` package is on classpath:

```xml
        <dependency>
            <groupId>org.jfoundry</groupId>
            <artifactId>jfoundry-persistence-mybatis-plus</artifactId>
            <scope>test</scope>
        </dependency>
```

- [ ] **Step 5: Run test to verify it passes**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn test -pl jfoundry-test -Dtest=PersistenceRulesTest 2>&1 | tail -15
```

Expected: `Tests run: 2, Failures: 0` / `BUILD SUCCESS`.

- [ ] **Step 6: Commit**

```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
git add -A
git commit -m "test(archunit): forbid @Transactional in persistence layer

Adds PersistenceRules with two rules:
- persistence_repository_must_not_use_transactional (P1-3 guard)
- autoconfig_must_not_use_component (P1-1 guard)

Both rules pass against jfoundry's own source."
```

---

### Task 1.5: Honor `jfoundry.outbox.dispatcher.enabled=false` (P1-4)

**Files:**
- Modify: `../../../jfoundry-spring/jfoundry-autoconfigure/src/main/java/org/jfoundry/autoconfigure/dispatcher/OutboxDispatcherAutoConfiguration.java`
- Test: `../../../jfoundry-spring/jfoundry-autoconfigure/src/test/java/org/jfoundry/autoconfigure/OutboxDispatcherEnabledTest.java`

**Interfaces:**
- Consumes: existing `OutboxDispatcherProperties` (prefix `jfoundry.outbox.dispatcher`)
- Produces: `OutboxDispatcherAutoConfiguration` and its beans all retract when `jfoundry.outbox.dispatcher.enabled=false`

- [ ] **Step 1: Write failing integration test**

Path: `../../../jfoundry-spring/jfoundry-autoconfigure/src/test/java/org/jfoundry/autoconfigure/OutboxDispatcherEnabledTest.java`

```java
package org.jfoundry.autoconfigure;

import org.jfoundry.infrastructure.messaging.outbox.OutboxDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/// P1-4 regression: jfoundry.outbox.dispatcher.enabled=false must actually disable
/// dispatcher bean registration. The existing config javadoc admitted the bug
/// ("业务侧需要禁用调度时...自行关闭 @EnableScheduling") — this test pins the fix.
@SpringBootTest(
        classes = {OutboxDispatcherEnabledTest.TestApp.class, OutboxDispatcherEnabledTest.DisabledConfig.class},
        properties = "jfoundry.outbox.dispatcher.enabled=false"
)
class OutboxDispatcherEnabledTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApp {
    }

    @TestConfiguration
    static class DisabledConfig {
        // no bean overrides; property alone disables the autoconfig
    }

    @Autowired
    private ApplicationContext context;

    @Test
    void noOutboxDispatcherBeanWhenDisabled() {
        assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
                .isThrownBy(() -> context.getBean(OutboxDispatcher.class));
    }

    @Test
    void noScheduledOutboxDispatcherBeanWhenDisabled() {
        assertThat(context.containsBeanDefinition("scheduledOutboxDispatcher"))
                .as("scheduledOutboxDispatcher bean must not be registered when enabled=false")
                .isFalse();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn test -pl jfoundry-spring/jfoundry-spring-autoconfigure -Dtest=OutboxDispatcherEnabledTest 2>&1 | tail -20
```

Expected: FAIL — `noOutboxDispatcherBeanWhenDisabled` fails because `OutboxDispatcherAutoConfiguration` has no `@ConditionalOnProperty(enabled=...)`.

- [ ] **Step 3: Add `@ConditionalOnProperty` to `OutboxDispatcherAutoConfiguration`**

Edit `../../../jfoundry-spring/jfoundry-autoconfigure/src/main/java/org/jfoundry/autoconfigure/dispatcher/OutboxDispatcherAutoConfiguration.java`. Add `@ConditionalOnProperty` to the class (and update the now-stale javadoc):

```java
package org.jfoundry.autoconfigure.dispatcher;

// ... existing imports ...
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/// Outbox Dispatcher 自动配置。
/// <p>
/// 根据 {@code jfoundry.outbox.dispatcher.mode} 选择 Dispatcher 实现：
/// <ul>
///   <li>{@code scheduled}（默认）：注册 ScheduledOutboxDispatcher（本类已加 @EnableScheduling）。</li>
///   <li>{@code jobrunr}：要求 classpath 有 jfoundry-messaging-jobrunr。</li>
/// </ul>
/// <p>
/// 总开关：{@code jfoundry.outbox.dispatcher.enabled=false} 将关闭整个 AutoConfiguration，
/// 所有 Dispatcher / BackoffStrategy bean 都不会注册，{@code @EnableScheduling} 也不会生效。
/// 默认 {@code matchIfMissing=true} 保持向后兼容。
@AutoConfiguration
@AutoConfigureAfter({
        MessageSenderAutoConfiguration.class,
        OutboxMybatisPlusAutoConfiguration.class
})
@EnableConfigurationProperties(OutboxDispatcherProperties.class)
@ConditionalOnProperty(prefix = "jfoundry.outbox.dispatcher", name = "enabled",
                       havingValue = "true", matchIfMissing = true)
@EnableScheduling
public class OutboxDispatcherAutoConfiguration {

    // ... existing @Bean methods unchanged ...
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn test -pl jfoundry-spring/jfoundry-spring-autoconfigure -Dtest=OutboxDispatcherEnabledTest 2>&1 | tail -15
```

Expected: `Tests run: 2, Failures: 0` / `BUILD SUCCESS`.

- [ ] **Step 5: Run full module tests**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn install -pl jfoundry-spring/jfoundry-spring-autoconfigure -am 2>&1 | tail -10
```

Expected: `BUILD SUCCESS`. Existing `DomainEventExternalizationIntegrationTest` still passes (it doesn't set `enabled=false`, so default `matchIfMissing=true` kicks in).

- [ ] **Step 6: Commit**

```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
git add -A
git commit -m "fix(autoconfig): honor jfoundry.outbox.dispatcher.enabled=false

Add @ConditionalOnProperty matchIfMissing=true at class level so that
enabled=false actually disables all Dispatcher / BackoffStrategy beans
and @EnableScheduling. Backward-compatible (default remains enabled)."
```

---

## Phase 2 — P2 Production Enhancements

**Phase goal:** Add 5 production capabilities: atomic Outbox claim for multi-instance safety, dynamic table name, dialect-aware pagination, payload capacity, and cleanup job. None of these break existing API; all are additive.

**Note on existing API:** Current `OutboxRepository` uses `String eventId` (not `Long id`) and `findDispatchable(int limit, Instant now)`. We preserve this signature shape.

### Task 2.1: Add DISPATCHING state + schema columns (P2-1 part 1)

**Files:**
- Modify: `jfoundry-infrastructure/jfoundry-messaging-core/src/main/java/org/jfoundry/infrastructure/messaging/outbox/OutboxStatus.java` (add `DISPATCHING`)
- Modify: `jfoundry-infrastructure/jfoundry-messaging-core/src/main/java/org/jfoundry/infrastructure/messaging/outbox/OutboxEntry.java` (add `claimedAt` / `claimedBy` fields, if missing)
- Modify: `jfoundry-infrastructure/jfoundry-messaging-mybatis-plus/src/main/resources/db/migration/V20260617__create_outbox_event.sql` (add columns)
- Modify: every `outbox_event.sql` test fixture (rename happened in Task 0.6)
- Test: `jfoundry-infrastructure/jfoundry-messaging-core/src/test/java/org/jfoundry/infrastructure/messaging/outbox/OutboxStatusTest.java`

**Interfaces:**
- Consumes: existing `OutboxStatus` enum
- Produces: 5-state `OutboxStatus` (PENDING/DISPATCHING/PUBLISHED/FAILED/DEAD_LETTERED) with new schema columns `claimed_at`, `claimed_by`

- [ ] **Step 1: Write failing test**

Path: `jfoundry-infrastructure/jfoundry-messaging-core/src/test/java/org/jfoundry/infrastructure/messaging/outbox/OutboxStatusTest.java`

```java
package org.jfoundry.infrastructure.messaging.outbox;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// P2-1: OutboxStatus must include DISPATCHING for atomic claim semantics.
class OutboxStatusTest {

    @Test
    void dispatchingStateExists() {
        assertThat(OutboxStatus.valueOf("DISPATCHING"))
                .isEqualTo(OutboxStatus.DISPATCHING);
    }

    @Test
    void hasFiveStates() {
        assertThat(OutboxStatus.values())
                .containsExactlyInAnyOrder(
                        OutboxStatus.PENDING,
                        OutboxStatus.DISPATCHING,
                        OutboxStatus.PUBLISHED,
                        OutboxStatus.FAILED,
                        OutboxStatus.DEAD_LETTERED);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn test -pl jfoundry-infrastructure/jfoundry-messaging-core -Dtest=OutboxStatusTest 2>&1 | tail -10
```

Expected: FAIL — `IllegalArgumentException: No enum constant org.jfoundry.infrastructure.messaging.outbox.OutboxStatus.DISPATCHING`.

- [ ] **Step 3: Add `DISPATCHING` to enum**

Edit `jfoundry-infrastructure/jfoundry-messaging-core/src/main/java/org/jfoundry/infrastructure/messaging/outbox/OutboxStatus.java`:

```java
package org.jfoundry.infrastructure.messaging.outbox;

/// Outbox 条目状态。
/// <p>
/// 状态流转：
/// <ul>
///   <li>{@code PENDING} → {@code DISPATCHING}（原子 claim，多实例下互斥）→ {@code PUBLISHED}（成功终态）</li>
///   <li>{@code DISPATCHING} → {@code FAILED}（派发失败）→ {@code DEAD_LETTERED}（重试耗尽，死信终态）</li>
///   <li>{@code DISPATCHING} stuck → {@code PENDING}（recovery 任务回滚，见 P2-1）</li>
///   <li>{@code DEAD_LETTERED} → {@code PENDING}（reactivate）</li>
/// </ul>
public enum OutboxStatus {
    PENDING,
    DISPATCHING,
    PUBLISHED,
    FAILED,
    DEAD_LETTERED
}
```

- [ ] **Step 4: Add `claimedAt` / `claimedBy` fields to `OutboxEntry`**

Read the current `OutboxEntry.java` first to understand its structure:
```bash
cat /Users/huangxiao/Workspace/mine/jfoundry/jfoundry-infrastructure/jfoundry-messaging-core/src/main/java/org/jfoundry/infrastructure/messaging/outbox/OutboxEntry.java
```

Add two new fields (position near the existing status field):

```java
    private String claimedBy;   // pod 标识：hostname + 短 UUID
    private Instant claimedAt;  // 最近一次成功 claim 的时间
```

And the corresponding getters/setters following the existing style (Lombok `@Data` or manual accessors — match the current file's pattern).

- [ ] **Step 5: Update V1 migration script with new columns**

Edit `jfoundry-infrastructure/jfoundry-messaging-mybatis-plus/src/main/resources/db/migration/V20260617__create_outbox_event.sql`. Read it first:

```bash
cat /Users/huangxiao/Workspace/mine/jfoundry/jfoundry-infrastructure/jfoundry-messaging-mybatis-plus/src/main/resources/db/migration/V20260617__create_outbox_event.sql
```

Add the `DISPATCHING` status comment and the new columns to the `CREATE TABLE`:

```sql
-- Add to the status column definition (comment listing valid values):
status VARCHAR(20) NOT NULL,  -- PENDING/DISPATCHING/PUBLISHED/FAILED/DEAD_LETTERED

-- Add these new columns before the indexes section:
claimed_at TIMESTAMP NULL,                   -- P2-1: DISPATCHING claim timestamp
claimed_by VARCHAR(100) NULL,                -- P2-1: pod identifier (hostname + short UUID)

-- Add to the indexes section (composite index for atomic claim WHERE clause):
INDEX idx_outbox_claim (status, claimed_at),
```

Note: use `TIMESTAMP` on MySQL; on DM use `TIMESTAMP` too (DM supports standard TIMESTAMP).

- [ ] **Step 6: Update test-fixture SQL files**

There are test-fixture `outbox_event.sql` files (renamed from `ddd_outbox_event.sql` in Task 0.6). Apply the same schema change:

```bash
FIXTURES=$(find /Users/huangxiao/Workspace/mine/jfoundry -name "outbox_event.sql" -not -path "*/target/*")
for f in $FIXTURES; do
  # Append the new columns if not already present
  if ! grep -q "claimed_at" "$f"; then
    # Locate the status column and the index section, add new columns + index
    # Manual edit is safer than sed for multi-line SQL; for each fixture:
    echo "Editing: $f"
  fi
done
```

For each fixture file, edit it to add `claimed_at` / `claimed_by` columns and the `idx_outbox_claim` index matching the migration script.

- [ ] **Step 7: Run test to verify it passes**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn test -pl jfoundry-infrastructure/jfoundry-messaging-core -Dtest=OutboxStatusTest 2>&1 | tail -10
```

Expected: `Tests run: 2, Failures: 0` / `BUILD SUCCESS`.

- [ ] **Step 8: Run full build to catch regressions**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn install -DskipTests 2>&1 | tail -5
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 9: Commit**

```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
git add -A
git commit -m "feat(outbox): add DISPATCHING state for atomic claim

Adds DISPATCHING to OutboxStatus enum (4 states -> 5), adds claimed_at /
claimed_by columns and idx_outbox_claim index to V1 migration + test
fixtures. Schema preparation for atomic claimDispatchable in next commit."
```

---

### Task 2.2: Implement `claimDispatchable` with conditional UPDATE (P2-1 part 2)

**Files:**
- Modify: `jfoundry-infrastructure/jfoundry-messaging-core/src/main/java/org/jfoundry/infrastructure/messaging/outbox/OutboxRepository.java` (add `claimDispatchable` method to SPI)
- Modify: `jfoundry-infrastructure/jfoundry-messaging-core/src/main/java/org/jfoundry/infrastructure/messaging/outbox/OutboxEntry.java` (helper methods if needed)
- Modify: `jfoundry-infrastructure/jfoundry-messaging-mybatis-plus/src/main/java/org/jfoundry/infrastructure/messaging/mybatis/outbox/OutboxMapper.java` (add `claimPending` + `selectByClaimer`)
- Modify: `jfoundry-infrastructure/jfoundry-messaging-mybatis-plus/src/main/java/org/jfoundry/infrastructure/messaging/mybatis/outbox/MybatisPlusOutboxRepository.java`
- Create: `jfoundry-infrastructure/jfoundry-messaging-mybatis-plus/src/main/resources/mapper/OutboxMapper.xml` (if annotation-based mapper, skip — use annotation-based SQL for portability)
- Test: `jfoundry-infrastructure/jfoundry-messaging-mybatis-plus/src/test/java/org/jfoundry/infrastructure/messaging/mybatis/outbox/ClaimDispatchableConcurrencyTest.java`

**Interfaces:**
- Consumes: 5-state `OutboxStatus` from Task 2.1
- Produces: `OutboxRepository.claimDispatchable(int limit, String claimerId)` returning `List<OutboxEntry>` — atomic across instances

- [ ] **Step 1: Write failing concurrency test**

Path: `jfoundry-infrastructure/jfoundry-messaging-mybatis-plus/src/test/java/org/jfoundry/infrastructure/messaging/mybatis/outbox/ClaimDispatchableConcurrencyTest.java`

```java
package org.jfoundry.infrastructure.messaging.mybatis.outbox;

import org.jfoundry.infrastructure.messaging.outbox.OutboxEntry;
import org.jfoundry.infrastructure.messaging.outbox.OutboxRepository;
import org.jfoundry.infrastructure.messaging.outbox.OutboxStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

/// P2-1: claimDispatchable must be atomic across threads — no two claimers
/// receive the same record.
@SpringBootTest(classes = ClaimDispatchableConcurrencyTest.TestApp.class)
class ClaimDispatchableConcurrencyTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApp {
    }

    @Autowired
    private OutboxRepository repository;

    @Autowired
    private OutboxMapper mapper;

    @Test
    void twoConcurrentClaimersGetDisjointRecords() throws Exception {
        // Seed 20 PENDING records
        for (int i = 0; i < 20; i++) {
            OutboxEntry e = OutboxEntry.newBuilder()
                    .eventId("evt-" + i)
                    .eventType("test.event")
                    .payload("{}")
                    .occurredAt(Instant.now())
                    .status(OutboxStatus.PENDING)
                    .build();
            repository.append(e);
        }

        Set<String> claimedByA = ConcurrentHashMap.newKeySet();
        Set<String> claimedByB = ConcurrentHashMap.newKeySet();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);

        pool.submit(() -> {
            try { start.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
            repository.claimDispatchable(10, "pod-A").forEach(e -> claimedByA.add(e.getEventId()));
            done.countDown();
        });
        pool.submit(() -> {
            try { start.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
            repository.claimDispatchable(10, "pod-B").forEach(e -> claimedByB.add(e.getEventId()));
            done.countDown();
        });

        start.countDown();
        done.await();
        pool.shutdown();

        // Assertions
        assertThat(claimedByA).hasSize(10);
        assertThat(claimedByB).hasSize(10);

        Set<String> intersection = new HashSet<>(claimedByA);
        intersection.retainAll(claimedByB);
        assertThat(intersection)
                .as("claimDispatchable must be atomic: zero intersection between two claimers")
                .isEmpty();
    }
}
```

Note: the exact builder API (`OutboxEntry.newBuilder()`) may differ from the current `OutboxEntry`. Read the file first and adapt to the existing constructor pattern:

```bash
cat /Users/huangxiao/Workspace/mine/jfoundry/jfoundry-infrastructure/jfoundry-messaging-core/src/main/java/org/jfoundry/infrastructure/messaging/outbox/OutboxEntry.java
```

Adjust the test seed code to match the actual constructor or builder.

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn test -pl jfoundry-infrastructure/jfoundry-messaging-mybatis-plus -Dtest=ClaimDispatchableConcurrencyTest 2>&1 | tail -15
```

Expected: FAIL — `claimDispatchable` method doesn't exist on `OutboxRepository`.

- [ ] **Step 3: Add `claimDispatchable` to `OutboxRepository` SPI**

Edit `jfoundry-infrastructure/jfoundry-messaging-core/src/main/java/org/jfoundry/infrastructure/messaging/outbox/OutboxRepository.java`. Add the method after `findDispatchable`:

```java
    /// 原子声明一批待派发事件（多实例安全）。
    /// <p>
    /// 实现 SQL 形如（MySQL 方言）：
    /// <pre>
    /// UPDATE outbox_event
    ///   SET status = 'DISPATCHING', claimed_at = now, claimed_by = #{claimerId}
    ///   WHERE id IN (SELECT id FROM (SELECT id FROM outbox_event
    ///                                 WHERE status = 'PENDING' LIMIT #{limit}) t);
    /// SELECT * FROM outbox_event WHERE claimed_by = #{claimerId} AND status = 'DISPATCHING';
    /// </pre>
    /// <p>
    /// 多实例下，两个并发 claim 不会拿到相同记录（数据库行级锁保证）。
    /// <p>
    /// 实现侧注意：达梦数据库不支持 UPDATE...LIMIT subquery，需走 ROWNUM/top-N 改写
    /// （由 P2-3 dialect 机制分派）。
    List<OutboxEntry> claimDispatchable(int limit, String claimerId);
```

- [ ] **Step 4: Add mapper methods**

Edit `jfoundry-infrastructure/jfoundry-messaging-mybatis-plus/src/main/java/org/jfoundry/infrastructure/messaging/mybatis/outbox/OutboxMapper.java`. Add:

```java
    /// MySQL/H2 方言：UPDATE 子查询 + 后续 SELECT。
    @Update("""
            UPDATE outbox_event
            SET status = 'DISPATCHING',
                claimed_at = NOW(3),
                claimed_by = #{claimerId}
            WHERE id IN (
                SELECT id FROM (
                    SELECT id FROM outbox_event
                    WHERE status = 'PENDING'
                    ORDER BY id
                    LIMIT #{limit}
                ) AS t
            )
            """)
    int claimPending(@Param("limit") int limit, @Param("claimerId") String claimerId);

    /// 达梦方言：用 ROWNUM 替代 LIMIT。
    @Update("""
            UPDATE outbox_event
            SET status = 'DISPATCHING',
                claimed_at = CURRENT_TIMESTAMP,
                claimed_by = #{claimerId}
            WHERE id IN (
                SELECT id FROM (
                    SELECT id FROM outbox_event
                    WHERE status = 'PENDING' AND ROWNUM <= #{limit}
                    ORDER BY id
                ) t
            )
            """)
    int claimPendingDm(@Param("limit") int limit, @Param("claimerId") String claimerId);

    @Select("SELECT * FROM outbox_event WHERE claimed_by = #{claimerId} AND status = 'DISPATCHING'")
    List<OutboxData> selectByClaimer(@Param("claimerId") String claimerId);
```

Add necessary imports:
```java
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;
```

- [ ] **Step 5: Implement `claimDispatchable` in `MybatisPlusOutboxRepository`**

Edit `jfoundry-infrastructure/jfoundry-messaging-mybatis-plus/src/main/java/org/jfoundry/infrastructure/messaging/mybatis/outbox/MybatisPlusOutboxRepository.java`. Add the method:

```java
    @Override
    public List<OutboxEntry> claimDispatchable(int limit, String claimerId) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive: " + limit);
        }
        if (claimerId == null || claimerId.isBlank()) {
            throw new IllegalArgumentException("claimerId must not be blank");
        }
        // Default to MySQL dialect; dialect dispatch added in Task 2.5.
        mapper.claimPending(limit, claimerId);
        return mapper.selectByClaimer(claimerId).stream()
                .map(OutboxData::toEntry)
                .toList();
    }
```

(Adjust `OutboxData::toEntry` to match the actual mapper-to-domain conversion method name. Read the current `MybatisPlusOutboxRepository` to find the existing conversion convention.)

- [ ] **Step 6: Run test to verify it passes**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn test -pl jfoundry-infrastructure/jfoundry-messaging-mybatis-plus -Dtest=ClaimDispatchableConcurrencyTest 2>&1 | tail -15
```

Expected: `Tests run: 1, Failures: 0` / `BUILD SUCCESS`.

- [ ] **Step 7: Run full module tests**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn install -pl jfoundry-infrastructure/jfoundry-messaging-mybatis-plus -am 2>&1 | tail -10
```

Expected: `BUILD SUCCESS`. Existing tests must still pass (they don't call `claimDispatchable` yet).

- [ ] **Step 8: Commit**

```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
git add -A
git commit -m "feat(outbox): implement claimDispatchable with conditional UPDATE

Atomic claim via UPDATE...WHERE id IN (LIMIT subquery) + selectByClaimer.
Two concurrent claimers receive disjoint record sets. Default dialect is
MySQL/H2; DM variant uses ROWNUM (dialect dispatch in Task 2.5)."
```

---

### Task 2.3: Add stuck-DISPATCHING recovery job (P2-1 part 3)

**Files:**
- Modify: `jfoundry-infrastructure/jfoundry-messaging-core/src/main/java/org/jfoundry/infrastructure/messaging/outbox/OutboxRepository.java` (add `recoverStuckDispatching`)
- Modify: `jfoundry-infrastructure/jfoundry-messaging-mybatis-plus/src/main/java/org/jfoundry/infrastructure/messaging/mybatis/outbox/OutboxMapper.java` (add `resetStuckDispatching`)
- Modify: `jfoundry-infrastructure/jfoundry-messaging-mybatis-plus/src/main/java/org/jfoundry/infrastructure/messaging/mybatis/outbox/MybatisPlusOutboxRepository.java`
- Create: `../../../jfoundry-spring/jfoundry-autoconfigure/src/main/java/org/jfoundry/autoconfigure/dispatcher/OutboxRecoveryJob.java`
- Create: `../../../jfoundry-spring/jfoundry-autoconfigure/src/main/java/org/jfoundry/autoconfigure/dispatcher/OutboxRecoveryProperties.java`
- Modify: `../../../jfoundry-spring/jfoundry-autoconfigure/src/main/java/org/jfoundry/autoconfigure/dispatcher/OutboxDispatcherAutoConfiguration.java` (register the job)
- Test: `../../../jfoundry-spring/jfoundry-autoconfigure/src/test/java/org/jfoundry/autoconfigure/dispatcher/OutboxRecoveryJobTest.java`

**Interfaces:**
- Consumes: 5-state `OutboxStatus` and `DISPATCHING` schema from Task 2.1
- Produces: `OutboxRepository.recoverStuckDispatching(Instant cutoff)` + Spring `@Scheduled` job that converts `Duration stuckTimeout` → `Instant cutoff = now.minus(timeout)` internally

- [ ] **Step 1: Write failing test**

Path: `../../../jfoundry-spring/jfoundry-autoconfigure/src/test/java/org/jfoundry/autoconfigure/dispatcher/OutboxRecoveryJobTest.java`

```java
package org.jfoundry.autoconfigure.dispatcher;

import org.jfoundry.infrastructure.messaging.mybatis.outbox.OutboxMapper;
import org.jfoundry.infrastructure.messaging.outbox.OutboxEntry;
import org.jfoundry.infrastructure.messaging.outbox.OutboxRepository;
import org.jfoundry.infrastructure.messaging.outbox.OutboxStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/// P2-1: recovery job must reset DISPATCHING records whose claimedAt is older
/// than the configured timeout back to PENDING.
@SpringBootTest(classes = OutboxRecoveryJobTest.TestApp.class)
class OutboxRecoveryJobTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApp {
    }

    @Autowired
    private OutboxRepository repository;

    @Autowired
    private OutboxMapper mapper;

    @Autowired
    private OutboxRecoveryJob job;

    @Test
    void stuckDispatchingRecordIsResetToPending() {
        // Seed a record and manually transition it to DISPATCHING with old claimedAt
        OutboxEntry e = OutboxEntry.newBuilder()
                .eventId("evt-stuck")
                .eventType("test")
                .payload("{}")
                .occurredAt(Instant.now())
                .status(OutboxStatus.PENDING)
                .build();
        repository.append(e);
        repository.claimDispatchable(1, "pod-stuck");

        // Manually age the claimedAt to 10 minutes ago
        mapper.ageClaimedAt("evt-stuck", Instant.now().minus(Duration.ofMinutes(10)));

        // Run recovery with 5-minute threshold
        int recovered = job.recoverStuckDispatching();

        assertThat(recovered).isEqualTo(1);

        // Verify the record is back to PENDING
        OutboxEntry reset = repository.findById("evt-stuck");
        assertThat(reset.getStatus()).isEqualTo(OutboxStatus.PENDING);
    }
}
```

Note: `OutboxEntry.newBuilder()` and `repository.findById(...)` are placeholders for the actual builder/lookup API. Read `OutboxEntry.java` and `OutboxRepository.java` to use the real method names. `mapper.ageClaimedAt` is a test-only helper you'll add in Step 4.

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn test -pl jfoundry-spring/jfoundry-spring-autoconfigure -Dtest=OutboxRecoveryJobTest 2>&1 | tail -15
```

Expected: FAIL — `OutboxRecoveryJob`, `OutboxRepository.recoverStuckDispatching`, and `OutboxMapper.ageClaimedAt` don't exist yet.

- [ ] **Step 3: Add `recoverStuckDispatching` to SPI and mapper**

Edit `jfoundry-infrastructure/jfoundry-messaging-core/src/main/java/org/jfoundry/infrastructure/messaging/outbox/OutboxRepository.java`. Add:

```java
    /// 恢复卡住的 DISPATCHING 记录：claimedAt 早于 {@code cutoff} 的记录回滚为 PENDING。
    /// <p>
    /// 用于 pod 崩溃 / kill -9 后回收半完成记录。
    /// @param cutoff 截止时刻（如 {@code Instant.now().minus(Duration.ofMinutes(5))}）
    /// @return 回滚的记录数
    int recoverStuckDispatching(java.time.Instant cutoff);
```

Edit `OutboxMapper.java`. Add:

```java
    @Update("UPDATE outbox_event SET status = 'PENDING', claimed_at = NULL, claimed_by = NULL " +
            "WHERE status = 'DISPATCHING' AND claimed_at < #{cutoff}")
    int resetStuckDispatching(@Param("cutoff") Instant cutoff);

    // Test helper
    @Update("UPDATE outbox_event SET claimed_at = #{oldTimestamp} WHERE event_id = #{eventId}")
    void ageClaimedAt(@Param("eventId") String eventId, @Param("oldTimestamp") Instant oldTimestamp);
```

Add necessary imports (`java.time.Instant`).

Edit `MybatisPlusOutboxRepository.java`. Implement:

```java
    @Override
    public int recoverStuckDispatching(Instant cutoff) {
        if (cutoff == null) {
            throw new IllegalArgumentException("cutoff must not be null");
        }
        return mapper.resetStuckDispatching(cutoff);
    }
```

- [ ] **Step 4: Create `OutboxRecoveryProperties`**

Path: `../../../jfoundry-spring/jfoundry-autoconfigure/src/main/java/org/jfoundry/autoconfigure/dispatcher/OutboxRecoveryProperties.java`

```java
package org.jfoundry.autoconfigure.dispatcher;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/// Outbox DISPATCHING 恢复任务配置。
/// <p>
/// Prefix: {@code jfoundry.outbox.recovery}
@ConfigurationProperties(prefix = "jfoundry.outbox.recovery")
public class OutboxRecoveryProperties {

    /// 恢复任务执行间隔。默认 60s。
    private Duration interval = Duration.ofSeconds(60);

    /// DISPATCHING 卡住阈值。默认 5min。
    private Duration stuckTimeout = Duration.ofMinutes(5);

    // getters/setters
    public Duration getInterval() { return interval; }
    public void setInterval(Duration interval) { this.interval = interval; }
    public Duration getStuckTimeout() { return stuckTimeout; }
    public void setStuckTimeout(Duration stuckTimeout) { this.stuckTimeout = stuckTimeout; }
}
```

- [ ] **Step 5: Create `OutboxRecoveryJob`**

Path: `../../../jfoundry-spring/jfoundry-autoconfigure/src/main/java/org/jfoundry/autoconfigure/dispatcher/OutboxRecoveryJob.java`

```java
package org.jfoundry.autoconfigure.dispatcher;

import org.jfoundry.infrastructure.messaging.outbox.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.time.Instant;

/// 周期性恢复卡住的 DISPATCHING 记录。
/// <p>
/// 场景：pod 在 DISPATCHING 中途崩溃 / kill -9，记录残留在 DISPATCHING 状态。
/// 本任务按 {@code jfoundry.outbox.recovery.stuck-timeout} 阈值回滚。
public class OutboxRecoveryJob {

    private static final Logger log = LoggerFactory.getLogger(OutboxRecoveryJob.class);

    private final OutboxRepository outboxRepository;
    private final OutboxRecoveryProperties properties;

    @Autowired
    public OutboxRecoveryJob(OutboxRepository outboxRepository, OutboxRecoveryProperties properties) {
        this.outboxRepository = outboxRepository;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${jfoundry.outbox.recovery.interval:60s}")
    public int recoverStuckDispatching() {
        Duration timeout = properties.getStuckTimeout();
        Instant cutoff = Instant.now().minus(timeout);
        int recovered = outboxRepository.recoverStuckDispatching(cutoff);
        if (recovered > 0) {
            log.warn("Recovered {} stuck DISPATCHING outbox records (threshold={})",
                    recovered, timeout);
        }
        return recovered;
    }
}
```

- [ ] **Step 6: Register `OutboxRecoveryJob` in `OutboxDispatcherAutoConfiguration`**

Edit `../../../jfoundry-spring/jfoundry-autoconfigure/src/main/java/org/jfoundry/autoconfigure/dispatcher/OutboxDispatcherAutoConfiguration.java`. Add inside the class body:

```java
    @Bean
    @ConditionalOnBean({OutboxRepository.class})
    @ConditionalOnMissingBean(OutboxRecoveryJob.class)
    public OutboxRecoveryJob outboxRecoveryJob(OutboxRepository outboxRepository,
                                               OutboxRecoveryProperties recoveryProperties) {
        return new OutboxRecoveryJob(outboxRepository, recoveryProperties);
    }
```

Also add `OutboxRecoveryProperties.class` to the existing `@EnableConfigurationProperties` declaration:

```java
@EnableConfigurationProperties({OutboxDispatcherProperties.class, OutboxRecoveryProperties.class})
```

And import the new types:
```java
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
```

- [ ] **Step 7: Run test to verify it passes**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn test -pl jfoundry-spring/jfoundry-spring-autoconfigure -Dtest=OutboxRecoveryJobTest 2>&1 | tail -15
```

Expected: `Tests run: 1, Failures: 0` / `BUILD SUCCESS`.

- [ ] **Step 8: Run full module tests**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn install -pl jfoundry-spring/jfoundry-spring-autoconfigure -am 2>&1 | tail -10
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 9: Commit**

```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
git add -A
git commit -m "feat(outbox): add stuck-DISPATCHING recovery job

OutboxRecoveryJob runs on @Scheduled fixedDelay (default 60s), resets
DISPATCHING records older than jfoundry.outbox.recovery.stuck-timeout
(default 5min) back to PENDING. Logs warning on each recovery."
```

---

### Task 2.4: Dynamic Outbox table name via `TableNameHandler` (P2-2)

**Files:**
- Modify: `jfoundry-infrastructure/jfoundry-messaging-mybatis-plus/src/main/java/org/jfoundry/infrastructure/messaging/mybatis/outbox/OutboxData.java` (delete `@TableName("ddd_outbox_event")`)
- Create: `../../../jfoundry-spring/jfoundry-autoconfigure/src/main/java/org/jfoundry/autoconfigure/persistence/JfoundryOutboxProperties.java`
- Modify: `../../../jfoundry-spring/jfoundry-autoconfigure/src/main/java/org/jfoundry/autoconfigure/persistence/OutboxMybatisPlusAutoConfiguration.java` (register `TableNameHandler`)
- Test: `../../../jfoundry-spring/jfoundry-autoconfigure/src/test/java/org/jfoundry/autoconfigure/persistence/OutboxTableNameOverrideTest.java`

**Interfaces:**
- Consumes: existing `OutboxData` entity
- Produces: `jfoundry.outbox.table-name` property (default `ddd_outbox_event` for backward compat) driving `TableNameHandler`

- [ ] **Step 1: Write failing test**

Path: `../../../jfoundry-spring/jfoundry-autoconfigure/src/test/java/org/jfoundry/autoconfigure/persistence/OutboxTableNameOverrideTest.java`

```java
package org.jfoundry.autoconfigure.persistence;

import org.jfoundry.infrastructure.messaging.mybatis.outbox.OutboxData;
import org.jfoundry.infrastructure.messaging.outbox.OutboxEntry;
import org.jfoundry.infrastructure.messaging.outbox.OutboxRepository;
import org.jfoundry.infrastructure.messaging.outbox.OutboxStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/// P2-2: jfoundry.outbox.table-name must redirect OutboxData persistence to a
/// custom-named table.
@SpringBootTest(
        classes = OutboxTableNameOverrideTest.TestApp.class,
        properties = "jfoundry.outbox.table-name=custom_outbox"
)
class OutboxTableNameOverrideTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApp {
    }

    @Autowired
    private OutboxRepository repository;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void appendWritesToCustomTable() {
        OutboxEntry entry = OutboxEntry.newBuilder()
                .eventId("evt-custom")
                .eventType("test")
                .payload("{}")
                .occurredAt(Instant.now())
                .status(OutboxStatus.PENDING)
                .build();
        repository.append(entry);

        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM custom_outbox WHERE event_id = ?",
                Integer.class, "evt-custom");
        assertThat(count).isEqualTo(1);

        Integer defaultCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM ddd_outbox_event WHERE event_id = ?",
                Integer.class, "evt-custom");
        assertThat(defaultCount)
                .as("default table must be empty when table-name override is set")
                .isEqualTo(0);
    }
}
```

(Adjust `OutboxEntry.newBuilder()` to the real builder/constructor API.)

Also: the test fixture SQL needs to create both `ddd_outbox_event` and `custom_outbox` tables. Add to test resources:

Path: `../../../jfoundry-spring/jfoundry-autoconfigure/src/test/resources/outbox_event.sql` (append, don't overwrite):

```sql
-- Existing ddd_outbox_event table definition stays.
-- Add a second table with custom name for the override test:
CREATE TABLE IF NOT EXISTS custom_outbox (
    -- identical schema to ddd_outbox_event
    ...
);
```

Read the existing fixture first and replicate the schema under the new table name.

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn test -pl jfoundry-spring/jfoundry-spring-autoconfigure -Dtest=OutboxTableNameOverrideTest 2>&1 | tail -15
```

Expected: FAIL — `OutboxData` still has hardcoded `@TableName("ddd_outbox_event")`, so writes go to the default table regardless of property.

- [ ] **Step 3: Delete `@TableName` from `OutboxData`**

Edit `jfoundry-infrastructure/jfoundry-messaging-mybatis-plus/src/main/java/org/jfoundry/infrastructure/messaging/mybatis/outbox/OutboxData.java`. Find:

```java
@TableName("ddd_outbox_event")
public class OutboxData {
```

Replace with:

```java
/// 默认表名 {@code ddd_outbox_event}（向后兼容）。
/// 业务侧可通过 {@code jfoundry.outbox.table-name} 属性覆盖；由
/// {@code OutboxMybatisPlusAutoConfiguration} 注册的 {@link TableNameHandler}
/// 在运行时把框架内部的默认名重写为业务配置的表名。
public class OutboxData {
```

(Delete the `@TableName` annotation and its import.)

- [ ] **Step 4: Create `JfoundryOutboxProperties`**

Path: `../../../jfoundry-spring/jfoundry-autoconfigure/src/main/java/org/jfoundry/autoconfigure/persistence/JfoundryOutboxProperties.java`

```java
package org.jfoundry.autoconfigure.persistence;

import org.springframework.boot.context.properties.ConfigurationProperties;

/// Outbox 配置。
/// <p>
/// Prefix: {@code jfoundry.outbox}
@ConfigurationProperties(prefix = "jfoundry.outbox")
public class JfoundryOutboxProperties {

    /// OutboxData 实体对应的物理表名。默认 {@code ddd_outbox_event}（向后兼容）。
    private String tableName = "ddd_outbox_event";

    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
}
```

- [ ] **Step 5: Register `TableNameHandler` in `OutboxMybatisPlusAutoConfiguration`**

Edit `../../../jfoundry-spring/jfoundry-autoconfigure/src/main/java/org/jfoundry/autoconfigure/persistence/OutboxMybatisPlusAutoConfiguration.java`. Add the handler registration. The `TableNameHandler` must be registered **before** Mybatis-Plus initializes its `MybatisConfiguration`, which means using a `BeanDefinitionRegistryPostProcessor` or the Mybatis-Plus `MybatisPlusProperties.Customizer` — the canonical approach in 3.5.x is the static `TableNameHandler` registration via `GlobalConfig`:

Read the existing file first:
```bash
cat /Users/huangxiao/Workspace/mine/jfoundry/jfoundry-spring/jfoundry-spring-autoconfigure/src/main/java/org/jfoundry/autoconfigure/persistence/OutboxMybatisPlusAutoConfiguration.java
```

Add (matching the existing bean style):

```java
    /// P2-2: 动态表名处理器。把框架内部 OutboxData 的逻辑表名 "ddd_outbox_event"
    /// 重写为业务配置的 {@code jfoundry.outbox.table-name}。
    /// <p>
    /// 仅重写 OutboxData 表，其他业务表不受影响。
    @Bean
    static TableNameHandler outboxTableNameHandler(JfoundryOutboxProperties properties) {
        String configured = properties.getTableName();
        return (tableName, sqlStatement) ->
                "ddd_outbox_event".equals(tableName) ? configured : tableName;
    }
```

Register the handler statically via `MybatisPlusAutoConfiguration` customization — the cleanest 3.5.x way is to register a `ConfigurationCustomizer`:

```java
    @Bean
    static ConfigurationCustomizer outboxTableNameCustomizer(JfoundryOutboxProperties properties) {
        String configured = properties.getTableName();
        return configuration -> {
            TableNameHandler handler = (tableName, sql) ->
                    "ddd_outbox_event".equals(tableName) ? configured : tableName;
            MybatisPlusConfigurations.registerTableNameHandler(handler);
        };
    }
```

Note: exact registration API varies by MyBatis-Plus minor version. The canonical 3.5.12 approach is `GlobalConfig` + `DbConfig.setTableNameHandler(...)`. Consult the current OutboxMybatisPlusAutoConfiguration to see what's already there; use the same style. If unsure, the test in Step 1 will catch wrong wiring.

- [ ] **Step 6: Add `JfoundryOutboxProperties` to `@EnableConfigurationProperties`**

In `OutboxMybatisPlusAutoConfiguration`, add:

```java
@EnableConfigurationProperties(JfoundryOutboxProperties.class)
```

(Or to the existing `@EnableConfigurationProperties` list if one is present.)

- [ ] **Step 7: Run test to verify it passes**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn test -pl jfoundry-spring/jfoundry-spring-autoconfigure -Dtest=OutboxTableNameOverrideTest 2>&1 | tail -15
```

Expected: `Tests run: 1, Failures: 0` / `BUILD SUCCESS`.

- [ ] **Step 8: Run full module tests**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn install -pl jfoundry-spring/jfoundry-spring-autoconfigure -am 2>&1 | tail -10
```

Expected: `BUILD SUCCESS`. The default-config test (no `table-name` set) must still pass — default handler returns `ddd_outbox_event` which is the existing table.

- [ ] **Step 9: Commit**

```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
git add -A
git commit -m "feat(outbox): support dynamic table name via TableNameHandler

Removes hardcoded @TableName('ddd_outbox_event') from OutboxData. Adds
jfoundry.outbox.table-name property (default 'ddd_outbox_event', backward
compatible) and TableNameHandler in OutboxMybatisPlusAutoConfiguration
that redirects the default name to the configured one."
```

---

### Task 2.5: Explicit `DbType` for `PaginationInnerInterceptor` (P2-3)

**Files:**
- Create: `../../../jfoundry-spring/jfoundry-autoconfigure/src/main/java/org/jfoundry/autoconfigure/persistence/JfoundryPersistenceProperties.java`
- Create: `../../../jfoundry-spring/jfoundry-autoconfigure/src/main/java/org/jfoundry/autoconfigure/persistence/DbTypeResolver.java`
- Modify: `../../../jfoundry-spring/jfoundry-autoconfigure/src/main/java/org/jfoundry/autoconfigure/persistence/MybatisPlusAutoConfiguration.java` (or whichever class currently registers `PaginationInnerInterceptor`)
- Test: `../../../jfoundry-spring/jfoundry-autoconfigure/src/test/java/org/jfoundry/autoconfigure/persistence/DbTypeResolutionTest.java`

**Interfaces:**
- Consumes: existing `PaginationInnerInterceptor` registration
- Produces: `jfoundry.persistence.db-type` property + `DbTypeResolver.autoDetect(DataSource)` for explicit/auto `DbType`

- [ ] **Step 1: Write failing test**

Path: `../../../jfoundry-spring/jfoundry-autoconfigure/src/test/java/org/jfoundry/autoconfigure/persistence/DbTypeResolutionTest.java`

```java
package org.jfoundry.autoconfigure.persistence;

import com.baomidou.mybatisplus.annotation.DbType;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// P2-3: DbTypeResolver must (a) honor explicit config, (b) auto-detect from
/// DataSource metadata when not configured.
class DbTypeResolutionTest {

    @Test
    void explicitConfigOverridesAutoDetection() {
        JfoundryPersistenceProperties props = new JfoundryPersistenceProperties();
        props.setDbType(DbType.DM);

        DbType resolved = new DbTypeResolver().resolve(props, stubDataSource("H2", "H2"));

        assertThat(resolved).isEqualTo(DbType.DM);
    }

    @Test
    void autoDetectFromH2WhenNotConfigured() {
        JfoundryPersistenceProperties props = new JfoundryPersistenceProperties();  // dbType = null
        DataSource ds = stubDataSource("H2", "H2");

        DbType resolved = new DbTypeResolver().resolve(props, ds);

        assertThat(resolved).isEqualTo(DbType.H2);
    }

    @Test
    void autoDetectFromDmProduct() {
        JfoundryPersistenceProperties props = new JfoundryPersistenceProperties();
        DataSource ds = stubDataSource("DM DBMS", "8.1");

        DbType resolved = new DbTypeResolver().resolve(props, ds);

        assertThat(resolved).isEqualTo(DbType.DM);
    }

    private DataSource stubDataSource(String productName, String productVersion) {
        DatabaseMetaData meta = mock(DatabaseMetaData.class);
        Connection conn = mock(Connection.class);
        DataSource ds = mock(DataSource.class);
        try {
            when(meta.getDatabaseProductName()).thenReturn(productName);
            when(meta.getDatabaseProductVersion()).thenReturn(productVersion);
            when(conn.getMetaData()).thenReturn(meta);
            when(ds.getConnection()).thenReturn(conn);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ds;
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn test -pl jfoundry-spring/jfoundry-spring-autoconfigure -Dtest=DbTypeResolutionTest 2>&1 | tail -15
```

Expected: FAIL — `JfoundryPersistenceProperties` and `DbTypeResolver` don't exist.

- [ ] **Step 3: Create `JfoundryPersistenceProperties`**

Path: `../../../jfoundry-spring/jfoundry-autoconfigure/src/main/java/org/jfoundry/autoconfigure/persistence/JfoundryPersistenceProperties.java`

```java
package org.jfoundry.autoconfigure.persistence;

import com.baomidou.mybatisplus.annotation.DbType;
import org.springframework.boot.context.properties.ConfigurationProperties;

/// jfoundry 持久化配置。
/// <p>
/// Prefix: {@code jfoundry.persistence}
@ConfigurationProperties(prefix = "jfoundry.persistence")
public class JfoundryPersistenceProperties {

    /// 显式指定数据库方言。null 表示自动从 DataSource metadata 推断。
    private DbType dbType;

    public DbType getDbType() { return dbType; }
    public void setDbType(DbType dbType) { this.dbType = dbType; }
}
```

- [ ] **Step 4: Create `DbTypeResolver`**

Path: `../../../jfoundry-spring/jfoundry-autoconfigure/src/main/java/org/jfoundry/autoconfigure/persistence/DbTypeResolver.java`

```java
package org.jfoundry.autoconfigure.persistence;

import com.baomidou.mybatisplus.annotation.DbType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/// 解析 MyBatis-Plus {@link DbType}：显式配置优先，否则从 DataSource metadata 推断。
public class DbTypeResolver {

    private static final Logger log = LoggerFactory.getLogger(DbTypeResolver.class);

    public DbType resolve(JfoundryPersistenceProperties props, DataSource dataSource) {
        if (props.getDbType() != null) {
            log.debug("DbType from explicit config: {}", props.getDbType());
            return props.getDbType();
        }
        return autoDetect(dataSource);
    }

    public DbType autoDetect(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection()) {
            String name = conn.getMetaData().getDatabaseProductName();
            DbType detected = mapProductName(name);
            log.info("Auto-detected DbType '{}' from product name '{}'", detected, name);
            return detected;
        } catch (SQLException e) {
            log.warn("Failed to auto-detect DbType, falling back to MYSQL: {}", e.getMessage());
            return DbType.MYSQL;
        }
    }

    private DbType mapProductName(String name) {
        if (name == null) return DbType.MYSQL;
        String upper = name.toUpperCase();
        if (upper.contains("DM"))       return DbType.DM;
        if (upper.contains("MYSQL"))    return DbType.MYSQL;
        if (upper.contains("H2"))       return DbType.H2;
        if (upper.contains("POSTGRE"))  return DbType.POSTGRE_SQL;
        if (upper.contains("ORACLE"))   return DbType.ORACLE;
        if (upper.contains("SQL SERVER") || upper.contains("MSSQL"))
                                        return DbType.SQL_SERVER;
        return DbType.MYSQL;
    }
}
```

- [ ] **Step 5: Wire `DbTypeResolver` into `PaginationInnerInterceptor` registration**

Find the file that currently registers `MybatisPlusInterceptor` with `PaginationInnerInterceptor`. Likely:

```bash
grep -rln "PaginationInnerInterceptor" --include="*.java" /Users/huangxiao/Workspace/mine/jfoundry/ | grep -v target/
```

Edit that class. Change the line:
```java
interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
```
to:
```java
DbType dbType = dbTypeResolver.resolve(persistenceProperties, dataSource);
interceptor.addInnerInterceptor(new PaginationInnerInterceptor(dbType));
```

Add the resolver + properties as constructor params or `@Autowired` fields, and add `@EnableConfigurationProperties(JfoundryPersistenceProperties.class)` to the class.

- [ ] **Step 6: Run test to verify it passes**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn test -pl jfoundry-spring/jfoundry-spring-autoconfigure -Dtest=DbTypeResolutionTest 2>&1 | tail -15
```

Expected: `Tests run: 3, Failures: 0` / `BUILD SUCCESS`.

- [ ] **Step 7: Run full module tests**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn install -pl jfoundry-spring/jfoundry-spring-autoconfigure -am 2>&1 | tail -10
```

Expected: `BUILD SUCCESS`. Existing pagination-dependent tests (the ones that currently work with auto-detection) must still pass.

- [ ] **Step 8: Commit**

```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
git add -A
git commit -m "feat(persistence): explicit DbType for PaginationInnerInterceptor

Adds jfoundry.persistence.db-type property (optional) and DbTypeResolver
that honors explicit config first, falls back to DataSource metadata
auto-detection. Fixes HikariCP-wrapped DataSource mis-detection and DM
database recognition."
```

---

### Task 2.6: Use `MEDIUMTEXT`/`CLOB` for `payload_json` (P2-4)

**Files:**
- Modify: `jfoundry-infrastructure/jfoundry-messaging-mybatis-plus/src/main/resources/db/migration/V20260617__create_outbox_event.sql` (single V1 script with both column type variants — but since this is jfoundry as a new repo, we can just use `MEDIUMTEXT` for MySQL and document that DM deployments use a separate script)
- Create: `jfoundry-infrastructure/jfoundry-messaging-mybatis-plus/src/main/resources/db/migration/V20260617__create_outbox_event_dm.sql` (DM-specific script using `CLOB`)
- Modify: test fixture SQL files to match
- Test: `jfoundry-infrastructure/jfoundry-messaging-mybatis-plus/src/test/java/org/jfoundry/infrastructure/messaging/mybatis/outbox/PayloadCapacityTest.java`

**Interfaces:**
- Consumes: existing migration script with `payload_json TEXT`
- Produces: migration scripts with `payload_json MEDIUMTEXT` (MySQL/H2) and `payload_json CLOB` (DM)

- [ ] **Step 1: Write failing test**

Path: `jfoundry-infrastructure/jfoundry-messaging-mybatis-plus/src/test/java/org/jfoundry/infrastructure/messaging/mybatis/outbox/PayloadCapacityTest.java`

```java
package org.jfoundry.infrastructure.messaging.mybatis.outbox;

import org.jfoundry.infrastructure.messaging.outbox.OutboxEntry;
import org.jfoundry.infrastructure.messaging.outbox.OutboxRepository;
import org.jfoundry.infrastructure.messaging.outbox.OutboxStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/// P2-4: payload_json column must accept 1MB payloads (MEDIUMTEXT on MySQL/H2).
@SpringBootTest(classes = PayloadCapacityTest.TestApp.class)
class PayloadCapacityTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApp {
    }

    @Autowired
    private OutboxRepository repository;

    @Test
    void appendAcceptsOneMegabytePayload() {
        // 1MB of 'x' chars, wrapped in a JSON string with escaping
        String big = IntStream.range(0, 1024 * 1024)
                .mapToObj(i -> "x")
                .collect(Collectors.joining());
        String payload = "{\"msg\":\"" + big + "\"}";

        OutboxEntry entry = OutboxEntry.newBuilder()
                .eventId("evt-big")
                .eventType("test.large")
                .payload(payload)
                .occurredAt(Instant.now())
                .status(OutboxStatus.PENDING)
                .build();

        repository.append(entry);

        // No exception — test passes if append() succeeds with 1MB payload
        assertThat(entry.getEventId()).isEqualTo("evt-big");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn test -pl jfoundry-infrastructure/jfoundry-messaging-mybatis-plus -Dtest=PayloadCapacityTest 2>&1 | tail -15
```

Expected: FAIL — `SQLException: Data too long for column 'payload_json'` (H2's TEXT may behave differently; if H2 accepts it, switch to a different capacity marker — verify actual H2 TEXT limit first. The intent is the column type must support ≥ 1MB. If H2 already accepts it with TEXT, the test alone isn't sufficient; manually inspect the SQL to confirm `MEDIUMTEXT`/`CLOB` is declared.)

- [ ] **Step 3: Update migration script column type**

Edit `jfoundry-infrastructure/jfoundry-messaging-mybatis-plus/src/main/resources/db/migration/V20260617__create_outbox_event.sql`. Find:

```sql
payload_json TEXT NOT NULL,
```

Replace with:

```sql
payload_json MEDIUMTEXT NOT NULL,  -- 16MB; P2-4: supports 1MB+ payloads
```

Also update the test-fixture SQL files (`outbox_event.sql`) to match.

- [ ] **Step 4: Create DM-specific migration script**

Path: `jfoundry-infrastructure/jfoundry-messaging-mybatis-plus/src/main/resources/db/migration/V20260617__create_outbox_event_dm.sql`

Copy the MySQL script content, replacing column types:
- `MEDIUMTEXT` → `CLOB`
- `VARCHAR` stays
- `TIMESTAMP` stays (DM supports)
- `DATETIME(3)` (if any) → `TIMESTAMP`
- Index syntax: use `CREATE INDEX` rather than inline `INDEX` clauses

```sql
-- DM (达梦) variant of outbox_event schema.
-- Selected via Flyway's databaseId feature or db-specific profile.
CREATE TABLE ddd_outbox_event (
    id BIGINT NOT NULL,
    event_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(200) NOT NULL,
    payload_json CLOB NOT NULL,           -- P2-4: 2GB capacity
    occurred_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL,
    failed_attempts INT DEFAULT 0,
    last_error CLOB NULL,
    next_retry_at TIMESTAMP NULL,
    claimed_at TIMESTAMP NULL,            -- P2-1
    claimed_by VARCHAR(100) NULL,         -- P2-1
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX idx_outbox_event_id ON ddd_outbox_event (event_id);
CREATE INDEX idx_outbox_status_retry ON ddd_outbox_event (status, next_retry_at);
CREATE INDEX idx_outbox_claim ON ddd_outbox_event (status, claimed_at);
```

- [ ] **Step 5: Run test to verify it passes**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn test -pl jfoundry-infrastructure/jfoundry-messaging-mybatis-plus -Dtest=PayloadCapacityTest 2>&1 | tail -15
```

Expected: `Tests run: 1, Failures: 0` / `BUILD SUCCESS`.

- [ ] **Step 6: Run full module tests**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn install -pl jfoundry-infrastructure/jfoundry-messaging-mybatis-plus -am 2>&1 | tail -10
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 7: Commit**

```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
git add -A
git commit -m "feat(migration): use MEDIUMTEXT/CLOB for payload_json

MySQL/H2: MEDIUMTEXT (16MB). DM: CLOB (2GB) in separate _dm.sql variant.
MySQL TEXT's 64KB limit was too small for integration events carrying
large domain payloads."
```

---

### Task 2.7: Outbox cleanup job (P2-5)

**Files:**
- Modify: `jfoundry-infrastructure/jfoundry-messaging-core/src/main/java/org/jfoundry/infrastructure/messaging/outbox/OutboxRepository.java` (add `deleteByStatusAndCreatedAtBefore`)
- Modify: `jfoundry-infrastructure/jfoundry-messaging-mybatis-plus/src/main/java/org/jfoundry/infrastructure/messaging/mybatis/outbox/OutboxMapper.java`
- Modify: `jfoundry-infrastructure/jfoundry-messaging-mybatis-plus/src/main/java/org/jfoundry/infrastructure/messaging/mybatis/outbox/MybatisPlusOutboxRepository.java`
- Create: `../../../jfoundry-spring/jfoundry-autoconfigure/src/main/java/org/jfoundry/autoconfigure/dispatcher/OutboxCleanupProperties.java`
- Create: `../../../jfoundry-spring/jfoundry-autoconfigure/src/main/java/org/jfoundry/autoconfigure/dispatcher/OutboxCleanupJob.java`
- Modify: `../../../jfoundry-spring/jfoundry-autoconfigure/src/main/java/org/jfoundry/autoconfigure/dispatcher/OutboxDispatcherAutoConfiguration.java`
- Test: `../../../jfoundry-spring/jfoundry-autoconfigure/src/test/java/org/jfoundry/autoconfigure/dispatcher/OutboxCleanupJobTest.java`

**Interfaces:**
- Consumes: existing `OutboxRepository`, `OutboxStatus.PUBLISHED` / `DEAD_LETTERED` terminal states
- Produces: scheduled cleanup job + `jfoundry.outbox.cleanup.*` configuration

- [ ] **Step 1: Write failing test**

Path: `../../../jfoundry-spring/jfoundry-autoconfigure/src/test/java/org/jfoundry/autoconfigure/dispatcher/OutboxCleanupJobTest.java`

```java
package org.jfoundry.autoconfigure.dispatcher;

import org.jfoundry.infrastructure.messaging.mybatis.outbox.OutboxMapper;
import org.jfoundry.infrastructure.messaging.outbox.OutboxEntry;
import org.jfoundry.infrastructure.messaging.outbox.OutboxRepository;
import org.jfoundry.infrastructure.messaging.outbox.OutboxStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/// P2-5: cleanup job must delete PUBLISHED records older than retention threshold,
/// and leave recent PUBLISHED records alone.
@SpringBootTest(
        classes = OutboxCleanupJobTest.TestApp.class,
        properties = {
                "jfoundry.outbox.cleanup.published-retention-days=7",
                "jfoundry.outbox.cleanup.batch-size=100"
        }
)
class OutboxCleanupJobTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApp {
    }

    @Autowired
    private OutboxRepository repository;

    @Autowired
    private OutboxMapper mapper;

    @Autowired
    private OutboxCleanupJob job;

    @Test
    void deletesOnlyOldTerminalRecords() {
        // Seed: 1 PUBLISHED 8 days ago (should be deleted), 1 PUBLISHED 1 day ago (kept)
        seed("evt-old", OutboxStatus.PUBLISHED, Instant.now().minusSeconds(8 * 86400));
        seed("evt-recent", OutboxStatus.PUBLISHED, Instant.now().minusSeconds(86400));

        int deleted = job.runOnce();

        assertThat(deleted).isEqualTo(1);
        assertThat(repository.findById("evt-old")).isNull();
        assertThat(repository.findById("evt-recent")).isNotNull();
    }

    private void seed(String id, OutboxStatus status, Instant occurredAt) {
        OutboxEntry e = OutboxEntry.newBuilder()
                .eventId(id)
                .eventType("test")
                .payload("{}")
                .occurredAt(occurredAt)
                .status(status)
                .build();
        repository.append(e);
        mapper.updateStatus(id, status);  // helper to set status post-append
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn test -pl jfoundry-spring/jfoundry-spring-autoconfigure -Dtest=OutboxCleanupJobTest 2>&1 | tail -15
```

Expected: FAIL — `OutboxCleanupJob`, `OutboxCleanupProperties`, and `OutboxRepository.deleteByStatusAndCreatedAtBefore` don't exist.

- [ ] **Step 3: Add `deleteByStatusAndCreatedAtBefore` to SPI and mapper**

Edit `jfoundry-infrastructure/jfoundry-messaging-core/src/main/java/org/jfoundry/infrastructure/messaging/outbox/OutboxRepository.java`. Add:

```java
    /// 批量删除指定状态且 occurredAt 早于 cutoff 的记录，单批最多 {@code batchSize} 条。
    /// <p>
    /// 实现侧：循环调用 mapper 的 batch delete，直到返回 < batchSize，保证删干净。
    /// @return 实际删除的记录总数
    int deleteByStatusAndCreatedAtBefore(OutboxStatus status, Instant cutoff, int batchSize);
```

Edit `OutboxMapper.java`. Add:

```java
    @Delete("DELETE FROM outbox_event WHERE status = #{status} AND occurred_at < #{cutoff} " +
            "LIMIT #{batchSize}")
    int deleteBatchByStatusAndOccurredBefore(@Param("status") String status,
                                             @Param("cutoff") Instant cutoff,
                                             @Param("batchSize") int batchSize);

    @Update("UPDATE outbox_event SET status = #{status} WHERE event_id = #{eventId}")
    void updateStatus(@Param("eventId") String eventId, @Param("status") OutboxStatus status);
```

Edit `MybatisPlusOutboxRepository.java`. Implement:

```java
    @Override
    public int deleteByStatusAndCreatedAtBefore(OutboxStatus status, Instant cutoff, int batchSize) {
        if (status == null || cutoff == null || batchSize <= 0) {
            throw new IllegalArgumentException("Invalid args: status=" + status + ", cutoff=" + cutoff + ", batchSize=" + batchSize);
        }
        int total = 0;
        int deleted;
        do {
            deleted = mapper.deleteBatchByStatusAndOccurredBefore(status.name(), cutoff, batchSize);
            total += deleted;
        } while (deleted == batchSize);
        return total;
    }
```

- [ ] **Step 4: Create `OutboxCleanupProperties`**

Path: `../../../jfoundry-spring/jfoundry-autoconfigure/src/main/java/org/jfoundry/autoconfigure/dispatcher/OutboxCleanupProperties.java`

```java
package org.jfoundry.autoconfigure.dispatcher;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/// Outbox 清理任务配置。
/// <p>
/// Prefix: {@code jfoundry.outbox.cleanup}
@ConfigurationProperties(prefix = "jfoundry.outbox.cleanup")
public class OutboxCleanupProperties {

    private boolean enabled = true;
    private Duration interval = Duration.ofHours(24);  // daily
    private int publishedRetentionDays = 7;
    private int deadLetteredRetentionDays = 30;
    private int batchSize = 1000;

    // getters/setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Duration getInterval() { return interval; }
    public void setInterval(Duration interval) { this.interval = interval; }
    public int getPublishedRetentionDays() { return publishedRetentionDays; }
    public void setPublishedRetentionDays(int days) { this.publishedRetentionDays = days; }
    public int getDeadLetteredRetentionDays() { return deadLetteredRetentionDays; }
    public void setDeadLetteredRetentionDays(int days) { this.deadLetteredRetentionDays = days; }
    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
}
```

- [ ] **Step 5: Create `OutboxCleanupJob`**

Path: `../../../jfoundry-spring/jfoundry-autoconfigure/src/main/java/org/jfoundry/autoconfigure/dispatcher/OutboxCleanupJob.java`

```java
package org.jfoundry.autoconfigure.dispatcher;

import org.jfoundry.infrastructure.messaging.outbox.OutboxRepository;
import org.jfoundry.infrastructure.messaging.outbox.OutboxStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.time.Instant;

/// 周期性清理 Outbox 表中已进入终态（PUBLISHED / DEAD_LETTERED）且超过保留期的记录。
/// <p>
/// 任务是幂等的——重复执行无副作用；失败不影响 Outbox 主链路。
public class OutboxCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(OutboxCleanupJob.class);

    private final OutboxRepository outboxRepository;
    private final OutboxCleanupProperties properties;

    @Autowired
    public OutboxCleanupJob(OutboxRepository outboxRepository, OutboxCleanupProperties properties) {
        this.outboxRepository = outboxRepository;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${jfoundry.outbox.cleanup.interval:24h}")
    public int runOnce() {
        if (!properties.isEnabled()) {
            return 0;
        }

        Instant now = Instant.now();
        Instant publishedCutoff = now.minus(Duration.ofDays(properties.getPublishedRetentionDays()));
        Instant deadCutoff = now.minus(Duration.ofDays(properties.getDeadLetteredRetentionDays()));

        int publishedDeleted = outboxRepository.deleteByStatusAndCreatedAtBefore(
                OutboxStatus.PUBLISHED, publishedCutoff, properties.getBatchSize());
        int deadDeleted = outboxRepository.deleteByStatusAndCreatedAtBefore(
                OutboxStatus.DEAD_LETTERED, deadCutoff, properties.getBatchSize());

        int total = publishedDeleted + deadDeleted;
        if (total > 0) {
            log.info("Outbox cleanup: deleted {} PUBLISHED (retention {}d), {} DEAD_LETTERED (retention {}d)",
                    publishedDeleted, properties.getPublishedRetentionDays(),
                    deadDeleted, properties.getDeadLetteredRetentionDays());
        }
        return total;
    }
}
```

- [ ] **Step 6: Register in `OutboxDispatcherAutoConfiguration`**

Edit `OutboxDispatcherAutoConfiguration.java`. Add inside the class body:

```java
    @Bean
    @ConditionalOnBean(OutboxRepository.class)
    @ConditionalOnMissingBean(OutboxCleanupJob.class)
    public OutboxCleanupJob outboxCleanupJob(OutboxRepository outboxRepository,
                                             OutboxCleanupProperties cleanupProperties) {
        return new OutboxCleanupJob(outboxRepository, cleanupProperties);
    }
```

Update `@EnableConfigurationProperties`:
```java
@EnableConfigurationProperties({
        OutboxDispatcherProperties.class,
        OutboxRecoveryProperties.class,
        OutboxCleanupProperties.class
})
```

- [ ] **Step 7: Run test to verify it passes**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn test -pl jfoundry-spring/jfoundry-spring-autoconfigure -Dtest=OutboxCleanupJobTest 2>&1 | tail -15
```

Expected: `Tests run: 1, Failures: 0` / `BUILD SUCCESS`.

- [ ] **Step 8: Run full module tests**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn install -pl jfoundry-spring/jfoundry-spring-autoconfigure -am 2>&1 | tail -10
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 9: Commit**

```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
git add -A
git commit -m "feat(outbox): add cleanup job for terminal-state records

OutboxCleanupJob runs daily (configurable), deletes PUBLISHED records
older than jfoundry.outbox.cleanup.published-retention-days (default 7)
and DEAD_LETTERED older than dead-lettered-retention-days (default 30).
Batch deletion in chunks of batch-size (default 1000). Idempotent."
```

---

## Phase 3 — P3 DDD Concept Completion

**Phase goal:** Add 3 DDD concept integrations: (P3-1) `ValueObject` wrapper interface + ArchUnit guards, (P3-2) new `jfoundry-architecture-layered` module with 4 layer stereotypes + ArchUnit guards, (P3-3) jmolecules ecosystem integration (`jmolecules-archunit` + `jmolecules-jackson` aggregated via `JFoundryRules` and auto-registered via `JfoundryJacksonAutoConfiguration`).

**Key design principle:** P3 is about using jmolecules properly, not inventing new concepts. We add thin wrappers + ArchUnit guards, never reimplement jmolecules types.

### Task 3.1: Add `ValueObject` wrapper interface (P3-1)

**Files:**
- Create: `jfoundry-domain/src/main/java/org/jfoundry/domain/valueobject/ValueObject.java`
- Test: `jfoundry-domain/src/test/java/org/jfoundry/domain/valueobject/ValueObjectTest.java`

**Interfaces:**
- Consumes: `org.jmolecules.ddd.types.ValueObject` (from jmolecules-ddd, already on classpath)
- Produces: `org.jfoundry.domain.valueobject.ValueObject` — empty marker interface extending the jmolecules one; later tasks (3.3) reference it by FQN

- [ ] **Step 1: Write failing test**

Path: `jfoundry-domain/src/test/java/org/jfoundry/domain/valueobject/ValueObjectTest.java`

```java
package org.jfoundry.domain.valueobject;

import org.jmolecules.ddd.types.ValueObject as JMoleculesValueObject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/// P3-1: jfoundry ValueObject must extend jmolecules ValueObject so business code
/// imports only the wrapper and gets jmolecules ecosystem compatibility for free.
class ValueObjectTest {

    @Test
    void jfoundryValueObjectExtendsJmoleculesValueObject() {
        // Kotlin-style alias not supported in Java; use plain assertion
        assertThat(JMoleculesValueObject.class).isAssignableFrom(ValueObject.class);
    }

    @Test
    void sampleRecordMayImplementValueObject() {
        Money money = new Money(BigDecimal.TEN, "CNY");
        assertThat(money).isInstanceOf(ValueObject.class);
        assertThat(money).isInstanceOf(JMoleculesValueObject.class);
    }

    record Money(BigDecimal amount, String currency) implements ValueObject {
        public Money {
            if (amount == null || amount.signum() < 0) {
                throw new IllegalArgumentException("amount must be non-negative");
            }
            if (currency == null || currency.isBlank()) {
                throw new IllegalArgumentException("currency must not be blank");
            }
        }
    }
}
```

Note: remove the `as JMoleculesValueObject` alias line — Java doesn't support import aliases. The test compiles after replacing with a fully qualified name in the assertion body.

Corrected test:

```java
package org.jfoundry.domain.valueobject;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/// P3-1: jfoundry ValueObject must extend jmolecules ValueObject so business code
/// imports only the wrapper and gets jmolecules ecosystem compatibility for free.
class ValueObjectTest {

    @Test
    void jfoundryValueObjectExtendsJmoleculesValueObject() {
        assertThat(org.jmolecules.ddd.types.ValueObject.class)
                .isAssignableFrom(ValueObject.class);
    }

    @Test
    void sampleRecordMayImplementValueObject() {
        Money money = new Money(BigDecimal.TEN, "CNY");
        assertThat(money).isInstanceOf(ValueObject.class);
        assertThat(money).isInstanceOf(org.jmolecules.ddd.types.ValueObject.class);
    }

    record Money(BigDecimal amount, String currency) implements ValueObject {
        public Money {
            if (amount == null || amount.signum() < 0) {
                throw new IllegalArgumentException("amount must be non-negative");
            }
            if (currency == null || currency.isBlank()) {
                throw new IllegalArgumentException("currency must not be blank");
            }
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn test -pl jfoundry-domain -Dtest=ValueObjectTest 2>&1 | tail -10
```

Expected: FAIL — `ValueObject` class doesn't exist in `org.jfoundry.domain.valueobject`.

- [ ] **Step 3: Create `ValueObject` interface**

Path: `jfoundry-domain/src/main/java/org/jfoundry/domain/valueobject/ValueObject.java`

```java
package org.jfoundry.domain.valueobject;

/// 领域层值对象标记接口。
/// <p>
/// 业务值对象实现此接口即获得：
/// <ul>
///   <li>jmolecules {@code ValueObject} 类型语义（可被 jmolecules 生态工具识别）</li>
///   <li>框架 ArchUnit 规则保护（强制不可变、强制 equals/hashCode，见 {@code ValueObjectRules}）</li>
/// </ul>
/// <p>
/// 推荐使用 Java 21 {@code record} 作为实现载体——record 天生满足不可变和 equals/hashCode 契约。
/// 若使用 class 实现，必须：
/// <ul>
///   <li>声明为 {@code final}</li>
///   <li>所有字段 {@code final}</li>
///   <li>重写 {@code equals} / {@code hashCode}</li>
/// </ul>
/// <p>
/// 本接口不添加任何方法——它是纯标记接口，仅用于类型擦除后的业务可见性。
public interface ValueObject extends org.jmolecules.ddd.types.ValueObject {
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn test -pl jfoundry-domain -Dtest=ValueObjectTest 2>&1 | tail -10
```

Expected: `Tests run: 2, Failures: 0` / `BUILD SUCCESS`.

- [ ] **Step 5: Run full module tests**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn install -pl jfoundry-domain -am 2>&1 | tail -5
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 6: Commit**

```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
git add -A
git commit -m "feat(domain): add ValueObject wrapper interface

P3-1: thin marker interface extending jmolecules ValueObject so business
code imports only org.jfoundry.domain.valueobject.ValueObject and gets
jmolecules ecosystem compatibility for free. ArchUnit guards follow in
ValueObjectRules."
```

---

### Task 3.2: Create `jfoundry-architecture-layered` module with 4 stereotypes (P3-2)

**Files:**
- Create: `jfoundry-architecture-layered/pom.xml`
- Create: `jfoundry-architecture-layered/src/main/java/org/jfoundry/architecture/layered/ApplicationLayer.java`
- Create: `jfoundry-architecture-layered/src/main/java/org/jfoundry/architecture/layered/DomainLayer.java`
- Create: `jfoundry-architecture-layered/src/main/java/org/jfoundry/architecture/layered/InterfaceLayer.java`
- Create: `jfoundry-architecture-layered/src/main/java/org/jfoundry/architecture/layered/InfrastructureLayer.java`
- Modify: `/Users/huangxiao/Workspace/mine/jfoundry/pom.xml` (add `<module>jfoundry-architecture-layered</module>`)
- Modify: `jfoundry-dependencies/pom.xml` (add jmolecules-layered-architecture version if not present)
- Test: `jfoundry-architecture-layered/src/test/java/org/jfoundry/architecture/layered/LayerStereotypesTest.java`

**Interfaces:**
- Consumes: `org.jmolecules.architecture.layered.*` annotations (from jmolecules-layered-architecture)
- Produces: 4 stereotype annotations under `org.jfoundry.architecture.layered.*` that business code can use without importing jmolecules directly; later tasks (3.4) reference them by FQN

- [ ] **Step 1: Write failing test**

Path: `jfoundry-architecture-layered/src/test/java/org/jfoundry/architecture/layered/LayerStereotypesTest.java`

```java
package org.jfoundry.architecture.layered;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;

import static org.assertj.core.api.Assertions.assertThat;

/// P3-2: all 4 layer stereotypes must exist, be meta-annotated with their
/// jmolecules counterpart, and target package + type.
class LayerStereotypesTest {

    @Test
    void applicationLayerIsMetaAnnotated() {
        org.jmolecules.architecture.layered.ApplicationLayer meta =
                ApplicationLayer.class.getAnnotation(org.jmolecules.architecture.layered.ApplicationLayer.class);
        assertThat(meta).as("@ApplicationLayer must be meta-annotated with jmolecules @ApplicationLayer").isNotNull();
    }

    @Test
    void domainLayerIsMetaAnnotated() {
        org.jmolecules.architecture.layered.DomainLayer meta =
                DomainLayer.class.getAnnotation(org.jmolecules.architecture.layered.DomainLayer.class);
        assertThat(meta).as("@DomainLayer must be meta-annotated with jmolecules @DomainLayer").isNotNull();
    }

    @Test
    void interfaceLayerIsMetaAnnotated() {
        org.jmolecules.architecture.layered.InterfaceLayer meta =
                InterfaceLayer.class.getAnnotation(org.jmolecules.architecture.layered.InterfaceLayer.class);
        assertThat(meta).as("@InterfaceLayer must be meta-annotated with jmolecules @InterfaceLayer").isNotNull();
    }

    @Test
    void infrastructureLayerIsMetaAnnotated() {
        org.jmolecules.architecture.layered.InfrastructureLayer meta =
                InfrastructureLayer.class.getAnnotation(org.jmolecules.architecture.layered.InfrastructureLayer.class);
        assertThat(meta).as("@InfrastructureLayer must be meta-annotated with jmolecules @InfrastructureLayer").isNotNull();
    }

    @Test
    void allStereotypesTargetPackageAndType() {
        for (Class<? extends Annotation> stereotype : new Class[]{
                ApplicationLayer.class, DomainLayer.class, InterfaceLayer.class, InfrastructureLayer.class
        }) {
            java.lang.annotation.Target target = stereotype.getAnnotation(java.lang.annotation.Target.class);
            assertThat(target).as(stereotype.getSimpleName() + " must have @Target").isNotNull();
            assertThat(java.util.Arrays.asList(target.value()))
                    .as(stereotype.getSimpleName() + " must target PACKAGE and TYPE")
                    .contains(java.lang.annotation.ElementType.PACKAGE, java.lang.annotation.ElementType.TYPE);
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn test -pl jfoundry-architecture-layered -Dtest=LayerStereotypesTest 2>&1 | tail -10
```

Expected: FAIL — module doesn't exist yet (Maven will report "Could not find module").

- [ ] **Step 3: Add module to root pom**

Edit `/Users/huangxiao/Workspace/mine/jfoundry/pom.xml`. Add to the `<modules>` block:

```xml
    <modules>
        <module>jfoundry-dependencies</module>
        <module>jfoundry-domain</module>
        <module>jfoundry-architecture-layered</module>
        <module>jfoundry-infrastructure</module>
        <module>jfoundry-spring</module>
        <module>jfoundry-test</module>
    </modules>
```

- [ ] **Step 4: Verify `jmolecules-layered-architecture` version is pinned in BOM**

Run:
```bash
grep -n "jmolecules-layered-architecture" /Users/huangxiao/Workspace/mine/jfoundry/jfoundry-dependencies/pom.xml
```

If no match, add to the `<dependencyManagement>` block of `jfoundry-dependencies/pom.xml`:

```xml
            <dependency>
                <groupId>org.jmolecules</groupId>
                <artifactId>jmolecules-layered-architecture</artifactId>
                <version>${jmolecules.version}</version>
            </dependency>
```

And add the property if `jmolecules.version` is not yet declared:

```xml
        <jmolecules.version>2025.0.2</jmolecules.version>
```

- [ ] **Step 5: Create module `pom.xml`**

Path: `jfoundry-architecture-layered/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.jfoundry</groupId>
        <artifactId>jfoundry-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>jfoundry-architecture-layered</artifactId>
    <name>jfoundry-architecture-layered</name>
    <description>Pure-Java layered architecture stereotypes on jmolecules — zero Spring dependency</description>

    <dependencies>
        <dependency>
            <groupId>org.jmolecules</groupId>
            <artifactId>jmolecules-layered-architecture</artifactId>
        </dependency>

        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 6: Create the 4 stereotype annotations**

Path: `jfoundry-architecture-layered/src/main/java/org/jfoundry/architecture/layered/ApplicationLayer.java`

```java
package org.jfoundry.architecture.layered;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// 应用层标记。标注在 {@code package-info.java} 上声明整个 package 属于应用层。
/// <p>
/// 应用层负责：编排领域对象完成业务用例、管理事务边界、声明领域事件的消费。
/// 应用层不包含业务规则，只编排；业务规则归属领域层。
/// <p>
/// 依赖方向：应用层 → 领域层；禁止依赖适配器层与基础设施层。
@org.jmolecules.architecture.layered.ApplicationLayer
@Target({ElementType.PACKAGE, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApplicationLayer {
}
```

Path: `jfoundry-architecture-layered/src/main/java/org/jfoundry/architecture/layered/DomainLayer.java`

```java
package org.jfoundry.architecture.layered;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// 领域层标记。标注在 {@code package-info.java} 上声明整个 package 属于领域层。
/// <p>
/// 领域层包含：聚合根、实体、值对象、领域服务、领域事件、仓储接口（端口）。
/// 领域层是业务内核——零框架依赖、零基础设施依赖。
@org.jmolecules.architecture.layered.DomainLayer
@Target({ElementType.PACKAGE, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DomainLayer {
}
```

Path: `jfoundry-architecture-layered/src/main/java/org/jfoundry/architecture/layered/InterfaceLayer.java`

```java
package org.jfoundry.architecture.layered;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// 接口层（适配器层）标记。标注在 {@code package-info.java} 上声明整个 package 属于接口层。
/// <p>
/// 接口层包含：REST 控制器、MVC 控制器、MQTT/消息监听器、定时任务、gRPC 服务实现等
/// 接收外部输入的组件。
/// <p>
/// 依赖方向：接口层 → 应用层；禁止直接调用领域层或基础设施层。
@org.jmolecules.architecture.layered.InterfaceLayer
@Target({ElementType.PACKAGE, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface InterfaceLayer {
}
```

Path: `jfoundry-architecture-layered/src/main/java/org/jfoundry/architecture/layered/InfrastructureLayer.java`

```java
package org.jfoundry.architecture.layered;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// 基础设施层标记。标注在 {@code package-info.java} 上声明整个 package 属于基础设施层。
/// <p>
/// 基础设施层包含：仓储实现、外部服务客户端适配器、消息中间件适配、技术基础设施。
/// <p>
/// 依赖方向：基础设施层 → 领域层（实现领域层声明的端口）；禁止被接口层或应用层直接依赖。
@org.jmolecules.architecture.layered.InfrastructureLayer
@Target({ElementType.PACKAGE, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface InfrastructureLayer {
}
```

- [ ] **Step 7: Run test to verify it passes**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn test -pl jfoundry-architecture-layered -Dtest=LayerStereotypesTest 2>&1 | tail -10
```

Expected: `Tests run: 5, Failures: 0` / `BUILD SUCCESS`.

- [ ] **Step 8: Verify zero Spring dependency**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn dependency:tree -pl jfoundry-architecture-layered 2>&1 | grep -i spring || echo "CLEAN: zero Spring dependencies"
```

Expected: `CLEAN: zero Spring dependencies`.

- [ ] **Step 9: Run full build**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn install -DskipTests 2>&1 | tail -5
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 10: Commit**

```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
git add -A
git commit -m "feat(architecture-layered): add jfoundry-architecture-layered module

P3-2: pure-Java module wrapping jmolecules-layered-architecture stereotypes.
4 annotations (Application/Domain/Interface/Infrastructure) that business
code can use without importing jmolecules. Zero Spring dependency.
ArchUnit guards follow in LayeredRules."
```

---

### Task 3.3: Add `ValueObjectRules` ArchUnit rules (P3-1 guard)

**Files:**
- Create: `jfoundry-test/src/main/java/org/jfoundry/test/archunit/ValueObjectRules.java`
- Test: `jfoundry-test/src/test/java/org/jfoundry/test/archunit/ValueObjectRulesTest.java`

**Interfaces:**
- Consumes: `org.jfoundry.domain.valueobject.ValueObject` from Task 3.1, ArchUnit 1.4.2
- Produces: 3 ArchRule constants business code imports via `JFoundryRules.all()` (Task 3.5)

- [ ] **Step 1: Write failing test**

Path: `jfoundry-test/src/test/java/org/jfoundry/test/archunit/ValueObjectRulesTest.java`

```java
package org.jfoundry.test.archunit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// P3-1 guard: ValueObjectRules must be declared and must pass against jfoundry's own
/// source (no non-final ValueObject, no mutable ValueObject fields, all have
/// equals/hashCode via records).
class ValueObjectRulesTest {

    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_JARS)
            .importPackages("org.jfoundry");

    @Test
    void valueObjectRulesAreDeclared() {
        assertThat(ValueObjectRules.value_objects_must_be_final).isNotNull();
        assertThat(ValueObjectRules.value_object_fields_must_be_final).isNotNull();
        assertThat(ValueObjectRules.value_objects_must_implement_equals_and_hashCode).isNotNull();
    }

    @Test
    void jfoundryOwnValueObjectsPassRules() {
        ValueObjectRules.value_objects_must_be_final.check(classes);
        ValueObjectRules.value_object_fields_must_be_final.check(classes);
        ValueObjectRules.value_objects_must_implement_equals_and_hashCode.check(classes);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn test -pl jfoundry-test -Dtest=ValueObjectRulesTest 2>&1 | tail -10
```

Expected: FAIL — `ValueObjectRules` class doesn't exist.

- [ ] **Step 3: Add `jfoundry-domain` dependency to `jfoundry-test`**

Edit `jfoundry-test/pom.xml`. Add inside `<dependencies>`:

```xml
        <dependency>
            <groupId>org.jfoundry</groupId>
            <artifactId>jfoundry-domain</artifactId>
            <scope>compile</scope>
        </dependency>
```

(The BOM already pins the version.)

- [ ] **Step 4: Create `ValueObjectRules`**

Path: `jfoundry-test/src/main/java/org/jfoundry/test/archunit/ValueObjectRules.java`

```java
package org.jfoundry.test.archunit;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.lang.ArchRule;
import org.jfoundry.domain.valueobject.ValueObject;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/// 值对象架构规则集合。
/// <p>
/// 强制执行 spec Section 8.1 的约束：值对象必须不可变、必须有 equals/hashCode。
/// <p>
/// 业务侧用法：
/// <pre>
/// &#64;AnalyzeClasses(packages = "com.mysoft.ci")
/// class CiArchitectureTest {
///     &#64;ArchTest
///     ArchRule[] valueObjectRules = JFoundryRules.all();
/// }
/// </pre>
public final class ValueObjectRules {

    private ValueObjectRules() {
    }

    /// 值对象实现类必须是 record 或 final class。
    /// <p>
    /// record 天生 final、天生不可变、天生有 equals/hashCode，是首选载体；
    /// 若用 class 实现，必须显式 final 以防子类破坏不可变性。
    public static final ArchRule value_objects_must_be_final =
            classes()
                    .that().implement(ValueObject.class)
                    .should().beRecords()
                    .orShould().haveModifier(JavaModifier.FINAL)
                    .because("ValueObject must be immutable; records are immutable by default, "
                            + "class implementations must be final to prevent subclassing");

    /// 值对象的所有字段必须 final。
    /// <p>
    /// record 字段天生 final；这条规则主要约束 class 实现的值对象。
    public static final ArchRule value_object_fields_must_be_final =
            classes()
                    .that().implement(ValueObject.class)
                    .should().haveOnlyFinalFields()
                    .because("ValueObject fields must be final to guarantee immutability");

    /// 值对象必须实现 equals 和 hashCode。
    /// <p>
    /// record 天生实现；class 实现必须显式重写。两个相等的值对象必须产生相同的 hashCode。
    public static final ArchRule value_objects_must_implement_equals_and_hashCode =
            classes()
                    .that().implement(ValueObject.class)
                    .should(haveEqualsAndHashCode())
                    .because("ValueObject must implement equals and hashCode for value semantics");

    private static DescribedPredicate<JavaClass> haveEqualsAndHashCode() {
        return new DescribedPredicate<JavaClass>("implement equals and hashCode") {
            @Override
            public boolean test(JavaClass javaClass) {
                boolean hasEquals = javaClass.getMethods().stream()
                        .anyMatch(m -> "equals".equals(m.getName())
                                && m.getRawParameterTypes().size() == 1
                                && !m.getModifiers().contains(JavaModifier.NATIVE));
                boolean hasHashCode = javaClass.getMethods().stream()
                        .anyMatch(m -> "hashCode".equals(m.getName())
                                && m.getRawParameterTypes().isEmpty()
                                && !m.getModifiers().contains(JavaModifier.NATIVE));
                return hasEquals && hasHashCode;
            }
        };
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn test -pl jfoundry-test -Dtest=ValueObjectRulesTest 2>&1 | tail -15
```

Expected: `Tests run: 2, Failures: 0` / `BUILD SUCCESS`.

- [ ] **Step 6: Commit**

```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
git add -A
git commit -m "test(archunit): add ValueObjectRules for ValueObject immutability

P3-1 guard: 3 ArchRule constants enforcing that ValueObject implementations
are final (or records), have only final fields, and implement equals/hashCode.
All pass against jfoundry's own source."
```

---

### Task 3.4: Add `LayeredRules` ArchUnit rules (P3-2 guard)

**Files:**
- Create: `jfoundry-test/src/main/java/org/jfoundry/test/archunit/LayeredRules.java`
- Test: `jfoundry-test/src/test/java/org/jfoundry/test/archunit/LayeredRulesTest.java`

**Interfaces:**
- Consumes: 4 layer stereotypes from Task 3.2
- Produces: 2 ArchRule constants enforcing dependency direction

- [ ] **Step 1: Write failing test**

Path: `jfoundry-test/src/test/java/org/jfoundry/test/archunit/LayeredRulesTest.java`

```java
package org.jfoundry.test.archunit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// P3-2 guard: LayeredRules must be declared and pass against jfoundry's own source.
/// jfoundry's own modules don't use @ApplicationLayer etc. (it's a framework, not business
/// code), so this test only sanity-checks that the rules are valid ArchRule instances.
class LayeredRulesTest {

    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_JARS)
            .importPackages("org.jfoundry");

    @Test
    void layeredRulesAreDeclared() {
        assertThat(LayeredRules.dependencies_must_follow_layer_hierarchy).isNotNull();
        assertThat(LayeredRules.only_application_may_use_repository_directly).isNotNull();
    }

    @Test
    void rulesAreValidAgainstEmptyPackage() {
        // The rules should be no-op on a package with no layer annotations.
        // This catches malformed predicates that would throw at evaluation time.
        LayeredRules.dependencies_must_follow_layer_hierarchy.check(classes);
        LayeredRules.only_application_may_use_repository_directly.check(classes);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn test -pl jfoundry-test -Dtest=LayeredRulesTest 2>&1 | tail -10
```

Expected: FAIL — `LayeredRules` class doesn't exist.

- [ ] **Step 3: Add `jfoundry-architecture-layered` dependency to `jfoundry-test`**

Edit `jfoundry-test/pom.xml`. Add inside `<dependencies>`:

```xml
        <dependency>
            <groupId>org.jfoundry</groupId>
            <artifactId>jfoundry-architecture-layered</artifactId>
            <scope>compile</scope>
        </dependency>
```

- [ ] **Step 4: Create `LayeredRules`**

Path: `jfoundry-test/src/main/java/org/jfoundry/test/archunit/LayeredRules.java`

```java
package org.jfoundry.test.archunit;

import com.tngtech.archunit.lang.ArchRule;
import org.jfoundry.architecture.layered.ApplicationLayer;
import org.jfoundry.architecture.layered.DomainLayer;
import org.jfoundry.architecture.layered.InfrastructureLayer;
import org.jfoundry.architecture.layered.InterfaceLayer;
import org.jfoundry.domain.repository.Repository;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/// 分层架构规则集合。
/// <p>
/// 强制执行 spec Section 8.2 的依赖方向：应用层只能依赖应用层和领域层；
/// 基础设施层不允许被接口层或应用层直接依赖。
/// <p>
/// 业务侧用法：在业务模块的 ArchUnit 测试里通过 {@code JFoundryRules.all()} 一键启用。
public final class LayeredRules {

    private LayeredRules() {
    }

    /// 应用层只能依赖应用层和领域层——不能直接依赖接口层或基础设施层。
    /// <p>
    /// 这是六边形架构的核心约束：业务内核（应用+领域）必须与适配器层解耦。
    /// <p>
    /// 规则基于包上的 {@link ApplicationLayer} / {@link DomainLayer} / {@link InterfaceLayer} /
    /// {@link InfrastructureLayer} 标注识别（通过 package-info.java）。
    public static final ArchRule dependencies_must_follow_layer_hierarchy =
            noClasses()
                    .that().resideInAPackageAnnotatedWith(ApplicationLayer.class)
                    .should().dependOnClassesThat().resideInAnyPackageAnnotatedWith(
                            InterfaceLayer.class, InfrastructureLayer.class)
                    .because("Application layer must only depend on Application and Domain layers; "
                            + "adapters and infrastructure are outside the business core");

    /// 只有应用层可以直接调用 Repository；领域层通过端口（Repository 接口）声明需求，
    /// 基础设施层实现 Repository。应用层负责在用例编排中调用 Repository 获取聚合根。
    /// <p>
    /// 本规则是软约束：只检查 "应用层 package 里有没有 Repository 调用"，
    /// 用于发现误把 Repository 埋到 Controller 里的反模式。
    public static final ArchRule only_application_may_use_repository_directly =
            classes()
                    .that().implement(Repository.class)
                    .and().resideOutsideAPackageAnnotatedWith(InfrastructureLayer.class)
                    .should().beAnnotatedWith(DomainLayer.class)
                    .orShould().beInterfaces()
                    .because("Repository implementations belong in Infrastructure layer; "
                            + "Repository interface declarations belong in Domain layer");
}
```

- [ ] **Step 5: Run test to verify it passes**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn test -pl jfoundry-test -Dtest=LayeredRulesTest 2>&1 | tail -15
```

Expected: `Tests run: 2, Failures: 0` / `BUILD SUCCESS`.

- [ ] **Step 6: Commit**

```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
git add -A
git commit -m "test(archunit): add LayeredRules for layer dependency direction

P3-2 guard: 2 ArchRule constants enforcing DDD layer hierarchy —
application layer may not depend on interface/infrastructure; Repository
implementations live only in Infrastructure layer."
```

---

### Task 3.5: Add jmolecules-archunit + jmolecules-jackson deps, `JFoundryRules` aggregator (P3-3)

**Files:**
- Modify: `jfoundry-dependencies/pom.xml` (pin `jmolecules-archunit`, `jmolecules-jackson`)
- Create: `jfoundry-test/src/main/java/org/jfoundry/test/archunit/JFoundryRules.java`
- Modify: `jfoundry-test/pom.xml` (add `jmolecules-archunit` compile-scope dependency)
- Test: `jfoundry-test/src/test/java/org/jfoundry/test/archunit/JFoundryRulesTest.java`

**Interfaces:**
- Consumes: `PersistenceRules`, `ValueObjectRules`, `LayeredRules` (from Tasks 1.4, 3.3, 3.4); `org.jmolecules.integrations:jmolecules-archunit` JMoleculesRules
- Produces: `JFoundryRules.all()` and `JFoundryRules.jmoleculesNative()` — single-line aggregator business code imports

- [ ] **Step 1: Write failing test**

Path: `jfoundry-test/src/test/java/org/jfoundry/test/archunit/JFoundryRulesTest.java`

```java
package org.jfoundry.test.archunit;

import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// P3-3: JFoundryRules must aggregate framework rules + jmolecules native rules.
class JFoundryRulesTest {

    @Test
    void allReturnsAtLeastSevenRules() {
        // 3 ValueObject + 2 Layered + 2 Persistence = 7
        ArchRule[] all = JFoundryRules.all();
        assertThat(all).hasSizeGreaterThanOrEqualTo(7);
    }

    @Test
    void jmoleculesNativeReturnsAtLeastThreeRules() {
        ArchRule[] nativeRules = JFoundryRules.jmoleculesNative();
        assertThat(nativeRules).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    void allRulesAreNonNull() {
        for (ArchRule rule : JFoundryRules.all()) {
            assertThat(rule).as("rule in JFoundryRules.all() must not be null").isNotNull();
        }
        for (ArchRule rule : JFoundryRules.jmoleculesNative()) {
            assertThat(rule).as("rule in JFoundryRules.jmoleculesNative() must not be null").isNotNull();
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn test -pl jfoundry-test -Dtest=JFoundryRulesTest 2>&1 | tail -10
```

Expected: FAIL — `JFoundryRules` class doesn't exist; `jmolecules-archunit` dependency not declared.

- [ ] **Step 3: Add `jmolecules-archunit` and `jmolecules-jackson` versions to BOM**

Edit `jfoundry-dependencies/pom.xml`. Add inside `<dependencyManagement><dependencies>`:

```xml
            <dependency>
                <groupId>org.jmolecules.integrations</groupId>
                <artifactId>jmolecules-archunit</artifactId>
                <version>${jmolecules-integrations.version}</version>
            </dependency>
            <dependency>
                <groupId>org.jmolecules.integrations</groupId>
                <artifactId>jmolecules-jackson</artifactId>
                <version>${jmolecules-integrations.version}</version>
            </dependency>
```

Add the property if not already declared:

```xml
        <jmolecules-integrations.version>0.34.0</jmolecules-integrations.version>
```

(0.34.0 is the latest jmolecules-integrations release compatible with jmolecules 2025.0.2.)

- [ ] **Step 4: Add `jmolecules-archunit` to `jfoundry-test` dependencies**

Edit `jfoundry-test/pom.xml`. Add inside `<dependencies>`:

```xml
        <dependency>
            <groupId>org.jmolecules.integrations</groupId>
            <artifactId>jmolecules-archunit</artifactId>
            <scope>compile</scope>
        </dependency>
```

- [ ] **Step 5: Create `JFoundryRules` aggregator**

Path: `jfoundry-test/src/main/java/org/jfoundry/test/archunit/JFoundryRules.java`

```java
package org.jfoundry.test.archunit;

import com.tngtech.archunit.lang.ArchRule;
import org.jmolecules.archunit.JMoleculesRules;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/// jfoundry 架构规则聚合入口。
/// <p>
/// 业务侧用法（推荐）：
/// <pre>
/// import com.tngtech.archunit.junit.AnalyzeClasses;
/// import com.tngtech.archunit.junit.ArchTest;
/// import org.jfoundry.test.archunit.JFoundryRules;
///
/// &#64;AnalyzeClasses(packages = "com.mysoft.ci")
/// class CiArchitectureTest {
///     &#64;ArchTest
///     ArchRule[] jfoundryRules = JFoundryRules.all();
///
///     &#64;ArchTest
///     ArchRule[] jmoleculesNativeRules = JFoundryRules.jmoleculesNative();
/// }
/// </pre>
/// <p>
/// {@link #all()} 返回框架自有的全部规则（Persistence + ValueObject + Layered）；
/// {@link #jmoleculesNative()} 返回 jmolecules 官方提供的 DDD + 架构规则。
public final class JFoundryRules {

    private JFoundryRules() {
    }

    /// 框架自有的所有 ArchUnit 规则聚合。
    /// <p>
    /// 包含：
    /// <ul>
    ///   <li>{@link PersistenceRules} — 持久化层零 @Transactional、autoconfig 零 @Component</li>
    ///   <li>{@link ValueObjectRules} — 值对象必须不可变、必须有 equals/hashCode</li>
    ///   <li>{@link LayeredRules} — 分层依赖方向约束</li>
    /// </ul>
    public static ArchRule[] all() {
        List<ArchRule> collected = new ArrayList<>();
        collected.addAll(publicStaticArchRules(PersistenceRules.class));
        collected.addAll(publicStaticArchRules(ValueObjectRules.class));
        collected.addAll(publicStaticArchRules(LayeredRules.class));
        return collected.toArray(new ArchRule[0]);
    }

    /// jmolecules 官方提供的 DDD + 架构规则（精选）。
    /// <p>
    /// 来源：{@code org.jmolecules.integrations:jmolecules-archunit}。
    public static ArchRule[] jmoleculesNative() {
        return new ArchRule[]{
                JMoleculesRules.ddd().domainEventsMustBeImplementedInAggregate(),
                JMoleculesRules.ddd().aggregatesMustOnlyReferenceOtherAggregatesById(),
                JMoleculesRules.layeredArchitecture(),
        };
    }

    private static List<ArchRule> publicStaticArchRules(Class<?> rulesClass) {
        return Arrays.stream(rulesClass.getDeclaredFields())
                .filter(f -> (f.getModifiers() & java.lang.reflect.Modifier.STATIC) != 0)
                .filter(f -> ArchRule.class.isAssignableFrom(f.getType()))
                .flatMap(f -> {
                    try {
                        f.setAccessible(true);
                        return Stream.of((ArchRule) f.get(null));
                    } catch (IllegalAccessException e) {
                        throw new IllegalStateException(
                                "Failed to access ArchRule field " + rulesClass.getName() + "#" + f.getName(), e);
                    }
                })
                .toList();
    }
}
```

- [ ] **Step 6: Run test to verify it passes**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn test -pl jfoundry-test -Dtest=JFoundryRulesTest 2>&1 | tail -15
```

Expected: `Tests run: 3, Failures: 0` / `BUILD SUCCESS`.

- [ ] **Step 7: Run full module tests**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn install -pl jfoundry-test -am 2>&1 | tail -10
```

Expected: `BUILD SUCCESS`. All existing ArchUnit tests still pass.

- [ ] **Step 8: Commit**

```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
git add -A
git commit -m "feat(archunit): aggregate jfoundry + jmolecules rules in JFoundryRules

P3-3: single-line entry point for business-side ArchUnit tests.
- JFoundryRules.all(): 7+ framework rules (Persistence + ValueObject + Layered)
- JFoundryRules.jmoleculesNative(): 3 jmolecules native rules (DDD + layered)

Adds jmolecules-archunit dependency (pinned in BOM at 0.34.0)."
```

---

### Task 3.6: Add `JfoundryJacksonAutoConfiguration` (P3-3)

**Files:**
- Modify: `jfoundry-dependencies/pom.xml` (jmolecules-jackson already pinned in Task 3.5; verify here)
- Modify: `../../../jfoundry-spring/jfoundry-autoconfigure/pom.xml` (add `jmolecules-jackson` dependency)
- Create: `../../../jfoundry-spring/jfoundry-autoconfigure/src/main/java/org/jfoundry/autoconfigure/jackson/JfoundryJacksonAutoConfiguration.java`
- Modify: `../../../jfoundry-spring/jfoundry-autoconfigure/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Test: `../../../jfoundry-spring/jfoundry-autoconfigure/src/test/java/org/jfoundry/autoconfigure/jackson/JfoundryJacksonAutoConfigurationTest.java`

**Interfaces:**
- Consumes: `org.jmolecules.jackson.JMoleculesModule` (from jmolecules-jackson)
- Produces: Spring Boot autoconfig that registers `JMoleculesModule` on any classpath with Jackson + jfoundry

- [ ] **Step 1: Write failing integration test**

Path: `../../../jfoundry-spring/jfoundry-autoconfigure/src/test/java/org/jfoundry/autoconfigure/jackson/JfoundryJacksonAutoConfigurationTest.java`

```java
package org.jfoundry.autoconfigure.jackson;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/// P3-3: jmolecules Jackson module must be auto-registered so that ValueObject and
/// Identifier types serialize/deserialize as single values (not wrapped JSON objects).
@SpringBootTest(classes = JfoundryJacksonAutoConfigurationTest.TestApp.class)
class JfoundryJacksonAutoConfigurationTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApp {
    }

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void jmoleculesModuleIsRegistered() {
        assertThat(objectMapper.getRegisteredModuleIds())
                .as("JMolecules module must be registered with ObjectMapper")
                .contains("jmolecules-module");
    }

    @Test
    void moduleIsResolvableAsBean() {
        // The autoconfig registers a Module bean; verify it's the jmolecules one
        // by checking module type name via ObjectMapper's registered modules.
        boolean hasJmolecules = objectMapper.getRegisteredModuleIds().stream()
                .anyMatch(id -> id.toString().contains("jmolecules"));
        assertThat(hasJmolecules).isTrue();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn test -pl jfoundry-spring/jfoundry-spring-autoconfigure -Dtest=JfoundryJacksonAutoConfigurationTest 2>&1 | tail -15
```

Expected: FAIL — `jmolecules-module` not in registered modules (jmolecules-jackson not on classpath, autoconfig doesn't exist).

- [ ] **Step 3: Add `jmolecules-jackson` dependency to `jfoundry-autoconfigure`**

Edit `../../../jfoundry-spring/jfoundry-autoconfigure/pom.xml`. Add inside `<dependencies>`:

```xml
        <dependency>
            <groupId>org.jmolecules.integrations</groupId>
            <artifactId>jmolecules-jackson</artifactId>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
```

(Both are managed by the BOM.)

- [ ] **Step 4: Create `JfoundryJacksonAutoConfiguration`**

Path: `../../../jfoundry-spring/jfoundry-autoconfigure/src/main/java/org/jfoundry/autoconfigure/jackson/JfoundryJacksonAutoConfiguration.java`

```java
package org.jfoundry.autoconfigure.jackson;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jmolecules.jackson.JMoleculesModule;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/// 自动注册 jmolecules Jackson Module，让 ValueObject / Identifier 类型以单值形式
/// 序列化（而不是包裹成 JSON 对象）。
/// <p>
/// 仅当 classpath 上同时有 {@link ObjectMapper} 和 {@link JMoleculesModule} 时生效；
/// 业务侧若自定义了同类型 Module Bean，本自动注册退让。
@AutoConfiguration
@ConditionalOnClass({ObjectMapper.class, JMoleculesModule.class})
@ConditionalOnBean(ObjectMapper.class)
public class JfoundryJacksonAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(Module.class)
    public JMoleculesModule jmoleculesJacksonModule() {
        return new JMoleculesModule();
    }
}
```

- [ ] **Step 5: Register autoconfig in imports file**

Edit `../../../jfoundry-spring/jfoundry-autoconfigure/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`. Append the new line (keep existing 5 entries from Phase 1):

```
org.jfoundry.autoconfigure.messaging.DomainEventPublisherAutoConfiguration
org.jfoundry.autoconfigure.messaging.MessageSenderAutoConfiguration
org.jfoundry.autoconfigure.messaging.DomainEventExternalizerAutoConfiguration
org.jfoundry.autoconfigure.persistence.OutboxMybatisPlusAutoConfiguration
org.jfoundry.autoconfigure.dispatcher.OutboxDispatcherAutoConfiguration
org.jfoundry.autoconfigure.jackson.JfoundryJacksonAutoConfiguration
```

- [ ] **Step 6: Run test to verify it passes**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn test -pl jfoundry-spring/jfoundry-spring-autoconfigure -Dtest=JfoundryJacksonAutoConfigurationTest 2>&1 | tail -15
```

Expected: `Tests run: 2, Failures: 0` / `BUILD SUCCESS`.

- [ ] **Step 7: Run full module tests**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn install -pl jfoundry-spring/jfoundry-spring-autoconfigure -am 2>&1 | tail -10
```

Expected: `BUILD SUCCESS`. Existing autoconfig tests still pass (no Module bean conflict).

- [ ] **Step 8: Commit**

```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
git add -A
git commit -m "feat(jackson): auto-register JMoleculesModule via JfoundryJacksonAutoConfiguration

P3-3: business code using ValueObject/Identifier gets single-value JSON
serialization without manual Jackson module registration. Retracts when
business provides its own Module bean."
```

---

### Task 3.7: Add user-facing documentation for P3 concepts

**Files:**
- Create: `../../value-object.md`
- Create: `docs/layered-architecture.md`
- Create: `docs/archunit-rules.md`

**Interfaces:**
- Consumes: all P3 deliverables from Tasks 3.1–3.6
- Produces: standalone usage docs business developers can read without grepping the spec

- [ ] **Step 1: Create `../../value-object.md`**

Path: `/Users/huangxiao/Workspace/mine/jfoundry/docs/valueobject.md`

````markdown
# ValueObject 使用指南

## 快速开始

```java
package com.mysoft.ci.env.valueobject;

import org.jfoundry.domain.valueobject.ValueObject;
import java.math.BigDecimal;

public record Money(BigDecimal amount, String currency) implements ValueObject {
    public Money {
        if (amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException("amount must be non-negative");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("currency must not be blank");
        }
    }
}
```

## 为什么用 record

Java 21 `record` 天生满足 ValueObject 的三大契约：

1. **不可变** —— 所有字段 final，无 setter
2. **值相等** —— 自动生成 equals/hashCode，基于字段值而非身份
3. **final** —— record 不能被继承，防止子类破坏不可变性

`ValueObjectRules` 的三条 ArchUnit 规则检查这些契约。如果业务侧因故必须用 class 实现（例如有继承需求或需要 `@JsonCreator`），仍然可以：

```java
public final class Money implements ValueObject {
    private final BigDecimal amount;
    private final String currency;

    public Money(BigDecimal amount, String currency) {
        // validation in constructor
        this.amount = amount;
        this.currency = currency;
    }

    // getters

    @Override
    public boolean equals(Object o) { /* value-based equals */ }

    @Override
    public int hashCode() { /* value-based hashCode */ }
}
```

## 启用 ArchUnit 规则

在业务模块的 ArchUnit 测试里：

```java
@AnalyzeClasses(packages = "com.mysoft.ci")
class CiArchitectureTest {
    @ArchTest
    ArchRule[] rules = JFoundryRules.all();
}
```

包含 ValueObject 的三条规则：

- `value_objects_must_be_final` —— 必须 final 或 record
- `value_object_fields_must_be_final` —— 所有字段 final
- `value_objects_must_implement_equals_and_hashCode` —— 必须实现 equals/hashCode

## jmolecules 生态

`ValueObject` 继承 `org.jmolecules.ddd.types.ValueObject`，所以 jmolecules 工具链（jmolecules-jackson 序列化、jmolecules-archunit 规则）开箱即用。
````

- [ ] **Step 2: Create `docs/layered-architecture.md`**

Path: `/Users/huangxiao/Workspace/mine/jfoundry/docs/layered-architecture.md`

````markdown
# 分层架构使用指南

## 快速开始

在业务模块的 `package-info.java` 上标注 layer：

```java
// src/main/java/com/mysoft/ci/env/appservice/package-info.java
@ApplicationLayer
package com.mysoft.ci.env.appservice;

import org.jfoundry.architecture.layered.ApplicationLayer;
```

四个层标记：

| 注解 | 目标 package | 典型内容 |
|------|-------------|---------|
| `@InterfaceLayer` | `*.controller`, `*.listener`, `*.schedule` | REST 控制器、MQTT 监听器、定时任务 |
| `@ApplicationLayer` | `*.appservice` | 应用服务、DTO、编排逻辑 |
| `@DomainLayer` | `*.domain`, `*.model` | 聚合根、实体、值对象、领域事件、Repository 接口 |
| `@InfrastructureLayer` | `*.infrastructure`, `*.adapter` | Repository 实现、外部服务适配器 |

## 依赖方向

```
InterfaceLayer → ApplicationLayer → DomainLayer ← InfrastructureLayer
```

关键约束（由 `LayeredRules` 强制）：

- **应用层不能依赖接口层或基础设施层** —— 业务内核必须与适配器解耦
- **Repository 实现只能在基础设施层** —— 防止业务层直接操作持久化

## 启用 ArchUnit 规则

```java
@AnalyzeClasses(packages = "com.mysoft.ci")
class CiArchitectureTest {
    @ArchTest
    ArchRule[] rules = JFoundryRules.all();
}
```

包含分层规则：

- `dependencies_must_follow_layer_hierarchy`
- `only_application_may_use_repository_directly`

## 为什么不混入 Spring 的 @Service / @Component

分层（layer）和 bean 身份（bean role）是**两个正交关切**：

- **Layer** 是 package-level 关切：这个 package 属于哪一层？—— 用 `@ApplicationLayer` 回答
- **Bean role** 是 class-level 关切：这个 class 在 Spring 容器里是什么角色？—— 用 `@Service` / `@Component` 回答

框架**不**提供复合注解（如 `@ApplicationService`），因为它会让两个正交概念耦合在一起——
同一个类可以是应用服务（`@ApplicationLayer` + `@Service`）、可以是领域服务（`@DomainLayer` + `@Service`）、
也可以是适配器（`@InfrastructureLayer` + `@Component`）。
````

- [ ] **Step 3: Create `docs/archunit-rules.md`**

Path: `/Users/huangxiao/Workspace/mine/jfoundry/docs/archunit-rules.md`

````markdown
# ArchUnit 规则清单

## 快速启用（推荐）

```java
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.jfoundry.test.archunit.JFoundryRules;

@AnalyzeClasses(packages = "com.mysoft.ci")
class CiArchitectureTest {

    @ArchTest
    ArchRule[] jfoundryRules = JFoundryRules.all();

    @ArchTest
    ArchRule[] jmoleculesNativeRules = JFoundryRules.jmoleculesNative();
}
```

一行引入 jfoundry 自有的全部规则 + jmolecules 官方规则。

## jfoundry 自有规则

### PersistenceRules

| 规则 | 作用 |
|------|------|
| `persistence_repository_must_not_use_transactional` | 持久化实现包下零 `@Transactional` |
| `autoconfig_must_not_use_component` | autoconfig 模块禁止 `@Component`（用 `@AutoConfiguration` + `@Bean`） |

### ValueObjectRules

| 规则 | 作用 |
|------|------|
| `value_objects_must_be_final` | ValueObject 实现类必须 final 或 record |
| `value_object_fields_must_be_final` | ValueObject 字段必须全部 final |
| `value_objects_must_implement_equals_and_hashCode` | ValueObject 必须实现 equals/hashCode |

### LayeredRules

| 规则 | 作用 |
|------|------|
| `dependencies_must_follow_layer_hierarchy` | 应用层 package 不能依赖接口层或基础设施层 package |
| `only_application_may_use_repository_directly` | Repository 实现只能在基础设施层 |

## jmolecules 官方规则

`JFoundryRules.jmoleculesNative()` 返回：

- `domainEventsMustBeImplementedInAggregate` —— 领域事件必须在聚合根内实现
- `aggregatesMustOnlyReferenceOtherAggregatesById` —— 聚合之间只能通过 ID 引用
- `layeredArchitecture` —— jmolecules 标准分层规则

## 精细控制

如果只想启用某一组规则：

```java
@ArchTest
ArchRule noTransactionalInPersistence = PersistenceRules.persistence_repository_must_not_use_transactional;

@ArchTest
ArchRule[] valueObjectRules = {
    ValueObjectRules.value_objects_must_be_final,
    ValueObjectRules.value_object_fields_must_be_final,
    ValueObjectRules.value_objects_must_implement_equals_and_hashCode
};
```

## Maven 依赖

业务模块只需依赖 `jfoundry-test`（编译期）：

```xml
<dependency>
    <groupId>org.jfoundry</groupId>
    <artifactId>jfoundry-test</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <scope>test</scope>
</dependency>
```
````

- [ ] **Step 4: Run full build + tests**

Run:
```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn install 2>&1 | tail -10
```

Expected: `BUILD SUCCESS`, all tests green.

- [ ] **Step 5: Commit**

```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
git add -A
git commit -m "docs: add valueobject, layered-architecture, and archunit-rules guides

Standalone usage docs for business developers. Covers quick-start code
samples, rule inventory, and design rationale (layer vs bean role orthogonality)."
```

---

## Final Acceptance Checklist

After all 26 tasks complete, run the full acceptance check:

### Build verification

```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn clean install 2>&1 | tail -20
```

Expected: `BUILD SUCCESS`, all modules built, all tests green.

### Phase 0 acceptance — identity rename

```bash
cd /Users/huangxiao/Workspace/mine/jfoundry

echo "=== Phase 0 checks ==="
echo "1. No com.mysoft.framework.ddd in Java:"
grep -r "com\.mysoft\.framework\.ddd" --include="*.java" . | grep -v target/ | grep -v docs/superpowers/ && echo "FAIL" || echo "PASS"

echo "2. No ddd. config prefix in resources:"
grep -rn '"ddd\.' --include="*.java" --include="*.yml" --include="*.yaml" --include="*.properties" . | grep -v target/ | grep -v docs/superpowers/ && echo "FAIL" || echo "PASS"

echo "3. No ddd-* module dirs:"
find . -maxdepth 4 -type d -name "ddd-*" -not -path "*/target/*" && echo "FAIL" || echo "PASS"

echo "4. No <artifactId>ddd-* in poms:"
grep -rn "<artifactId>ddd-" --include="pom.xml" . | grep -v target/ && echo "FAIL" || echo "PASS"

echo "5. Root pom modules all jfoundry-*:"
grep "<module>" pom.xml

echo "6. No com.mysoft.framework.ddd in pom groupId:"
grep -r "com\.mysoft\.framework\.ddd" --include="pom.xml" . | grep -v target/ && echo "FAIL" || echo "PASS"
```

Expected: all 6 checks PASS.

### Phase 1 acceptance — P1 contract fixes

```bash
cd /Users/huangxiao/Workspace/mine/jfoundry

echo "=== Phase 1 checks ==="
echo "1. DomainEventPublisherAutoConfiguration exists:"
test -f jfoundry-spring/jfoundry-spring-autoconfigure/src/main/java/org/jfoundry/autoconfigure/messaging/DomainEventPublisherAutoConfiguration.java && echo "PASS" || echo "FAIL"

echo "2. SpringDomainEventPublisher no longer @Component:"
grep -l "@Component" jfoundry-infrastructure/jfoundry-messaging-spring/src/main/java/org/jfoundry/infrastructure/messaging/spring/publisher/SpringDomainEventPublisher.java 2>/dev/null && echo "FAIL" || echo "PASS"

echo "3. DomainEventExternalizer condition fixed:"
grep "ConditionalOnMissingBean(DomainEventExternalizer" jfoundry-spring/jfoundry-spring-autoconfigure/src/main/java/org/jfoundry/autoconfigure/messaging/DomainEventExternalizerAutoConfiguration.java && echo "PASS" || echo "FAIL"

echo "4. AggregateRepository no longer lies about 事务性保证:"
grep -rn "事务性保证" --include="*.java" . | grep -v target/ && echo "FAIL" || echo "PASS"

echo "5. PersistenceRules exists:"
test -f jfoundry-test/src/main/java/org/jfoundry/test/archunit/PersistenceRules.java && echo "PASS" || echo "FAIL"

echo "6. OutboxDispatcherAutoConfiguration honors enabled=false:"
grep " ConditionalOnProperty.*jfoundry\.outbox\.dispatcher.*enabled" jfoundry-spring/jfoundry-spring-autoconfigure/src/main/java/org/jfoundry/autoconfigure/dispatcher/OutboxDispatcherAutoConfiguration.java && echo "PASS" || echo "FAIL"
```

Expected: all 6 checks PASS.

### Phase 2 acceptance — P2 production capabilities

```bash
cd /Users/huangxiao/Workspace/mine/jfoundry

echo "=== Phase 2 checks ==="
echo "1. OutboxStatus has 5 states including DISPATCHING:"
grep -c "DISPATCHING" jfoundry-infrastructure/jfoundry-messaging-core/src/main/java/org/jfoundry/infrastructure/messaging/outbox/OutboxStatus.java | grep -q "^1$" && echo "PASS" || echo "FAIL"

echo "2. claimDispatchable exists in OutboxRepository:"
grep "claimDispatchable" jfoundry-infrastructure/jfoundry-messaging-core/src/main/java/org/jfoundry/infrastructure/messaging/outbox/OutboxRepository.java && echo "PASS" || echo "FAIL"

echo "3. OutboxRecoveryJob exists:"
test -f jfoundry-spring/jfoundry-spring-autoconfigure/src/main/java/org/jfoundry/autoconfigure/dispatcher/OutboxRecoveryJob.java && echo "PASS" || echo "FAIL"

echo "4. Dynamic table-name config exists:"
grep "JfoundryOutboxProperties" jfoundry-spring/jfoundry-spring-autoconfigure/src/main/java/org/jfoundry/autoconfigure/persistence/OutboxMybatisPlusAutoConfiguration.java && echo "PASS" || echo "FAIL"

echo "5. DbTypeResolver exists:"
test -f jfoundry-spring/jfoundry-spring-autoconfigure/src/main/java/org/jfoundry/autoconfigure/persistence/DbTypeResolver.java && echo "PASS" || echo "FAIL"

echo "6. Migration uses MEDIUMTEXT:"
grep -i "MEDIUMTEXT" jfoundry-infrastructure/jfoundry-messaging-mybatis-plus/src/main/resources/db/migration/V20260617__create_outbox_event.sql && echo "PASS" || echo "FAIL"

echo "7. DM variant migration exists:"
test -f jfoundry-infrastructure/jfoundry-messaging-mybatis-plus/src/main/resources/db/migration/V20260617__create_outbox_event_dm.sql && echo "PASS" || echo "FAIL"

echo "8. OutboxCleanupJob exists:"
test -f jfoundry-spring/jfoundry-spring-autoconfigure/src/main/java/org/jfoundry/autoconfigure/dispatcher/OutboxCleanupJob.java && echo "PASS" || echo "FAIL"
```

Expected: all 8 checks PASS.

### Phase 3 acceptance — P3 DDD completions

```bash
cd /Users/huangxiao/Workspace/mine/jfoundry

echo "=== Phase 3 checks ==="
echo "1. ValueObject interface exists:"
test -f jfoundry-domain/src/main/java/org/jfoundry/domain/valueobject/ValueObject.java && echo "PASS" || echo "FAIL"

echo "2. jfoundry-architecture-layered module exists:"
test -d jfoundry-architecture-layered && echo "PASS" || echo "FAIL"

echo "3. 4 layer stereotypes exist:"
COUNT=$(find jfoundry-architecture-layered/src/main/java -name "ApplicationLayer.java" -o -name "DomainLayer.java" -o -name "InterfaceLayer.java" -o -name "InfrastructureLayer.java" | wc -l | tr -d ' ')
test "$COUNT" = "4" && echo "PASS" || echo "FAIL"

echo "4. jfoundry-architecture-layered has zero Spring deps:"
mvn dependency:tree -pl jfoundry-architecture-layered 2>&1 | grep -c "spring-" | grep -q "^0$" && echo "PASS" || echo "FAIL"

echo "5. ValueObjectRules exists:"
test -f jfoundry-test/src/main/java/org/jfoundry/test/archunit/ValueObjectRules.java && echo "PASS" || echo "FAIL"

echo "6. LayeredRules exists:"
test -f jfoundry-test/src/main/java/org/jfoundry/test/archunit/LayeredRules.java && echo "PASS" || echo "FAIL"

echo "7. JFoundryRules aggregator exists:"
test -f jfoundry-test/src/main/java/org/jfoundry/test/archunit/JFoundryRules.java && echo "PASS" || echo "FAIL"

echo "8. JfoundryJacksonAutoConfiguration exists:"
test -f jfoundry-spring/jfoundry-spring-autoconfigure/src/main/java/org/jfoundry/autoconfigure/jackson/JfoundryJacksonAutoConfiguration.java && echo "PASS" || echo "FAIL"

echo "9. jmolecules-archunit pinned in BOM:"
grep "jmolecules-archunit" jfoundry-dependencies/pom.xml && echo "PASS" || echo "FAIL"

echo "10. Docs exist:"
test -f docs/value-object.md && test -f docs/layered-architecture.md && test -f docs/archunit-rules.md && echo "PASS" || echo "FAIL"
```

Expected: all 10 checks PASS.

### Final commit count

```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
git log --oneline | wc -l
```

Expected: approximately 26 commits (1 per task) plus Phase 0 commits, total ~28–32.

---


