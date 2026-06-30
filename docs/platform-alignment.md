# Platform Alignment Guide

This guide helps business applications align their platform dependencies before adopting `jfoundry` 1.x starters.

It is intentionally written as an application-side checklist. It does not prescribe private Maven repositories, Nacos namespaces, service names, database URLs, or company-specific SDKs.

## When To Use This Guide

Use this guide when an existing Spring Boot application wants to adopt `jfoundry` and already depends on some combination of:

- Spring Boot
- Spring Cloud
- Spring Cloud Alibaba / Nacos
- MyBatis-Plus
- JobRunr
- OpenFeign
- private platform BOMs or SDKs

Run this alignment before introducing runtime `jfoundry` starters. It is safe to introduce `jfoundry-dependencies` and `jfoundry-architecture-test` during the alignment step.

## Recommended 1.x Baseline

| Area | Recommended Baseline |
|------|----------------------|
| Java | 21 |
| Spring Boot | 3.5.16 |
| Spring Framework | 6.2.19 |
| Spring Cloud | 2025.0.3 |
| Spring Cloud Alibaba | 2025.0.0.0 |
| MyBatis-Plus | 3.5.16 |
| JobRunr | 8.7.1 |
| jfoundry | 1.0.0-SNAPSHOT until the first 1.x release is published |

`jfoundry` does not manage Spring Cloud Alibaba for business applications. Applications that use Nacos should import the matching Spring Cloud Alibaba BOM explicitly.

## Recommended BOM Order

Use one dependency-management block with a deliberate order:

```xml
<dependencyManagement>
    <dependencies>
        <!-- Application or company platform BOMs, if any. -->

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>${spring-boot.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>

        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>${spring-cloud.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>

        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-alibaba-dependencies</artifactId>
            <version>${spring-cloud-alibaba.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>

        <dependency>
            <groupId>org.jfoundry</groupId>
            <artifactId>jfoundry-dependencies</artifactId>
            <version>${jfoundry.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>

        <!-- Application-specific overrides, if they are intentional. -->
    </dependencies>
</dependencyManagement>
```

After importing a BOM, remove duplicate dependency-management entries for the same `groupId` and `artifactId` unless the application intentionally wants to override that BOM. A duplicate entry without a version can still hide the imported BOM result and lead Maven to report a missing dependency version.

## Minimal jfoundry Dependencies For Alignment

During platform alignment, prefer test-only `jfoundry` usage:

```xml
    <dependency>
        <groupId>org.jfoundry</groupId>
        <artifactId>jfoundry-architecture-test</artifactId>
        <scope>test</scope>
    </dependency>
```

Do not add runtime starters until the platform build, dependency tree, and local startup behavior are understood.

## Verification Commands

Replace `<profile>` and `<start-module>` with application-specific values:

```bash
mvn -P<profile> -nsu -pl <start-module> -am test -DskipTests
```

Expected result: Maven resolves the upgraded platform and exits with `BUILD SUCCESS`.

```bash
mvn -P<profile> -nsu -pl <start-module> -am dependency:tree \
  -Dincludes=org.springframework.boot,org.springframework.cloud,com.alibaba.cloud,com.baomidou,org.jobrunr,org.jfoundry
```

Expected result: the tree shows the intended Spring Boot, Spring Cloud, Spring Cloud Alibaba, MyBatis-Plus, JobRunr, and `jfoundry` versions.

```bash
mvn -P<profile> -nsu -pl <start-module> -am install -DskipTests
```

Expected result: all reactor artifacts needed by single-module startup are installed into the local Maven repository.

If your Maven mirror is slow or has stale SNAPSHOT metadata, `-nsu` can reduce noise while you isolate the actual compatibility problem. Remove `-nsu` when you need to refresh SNAPSHOT metadata.

## Local Startup Verification

For multi-module applications, install the reactor first and then start from the boot module:

```bash
mvn -P<profile> -nsu -pl <start-module> -am install -DskipTests
mvn -P<profile> -nsu -f <start-module>/pom.xml org.springframework.boot:spring-boot-maven-plugin:${spring-boot.version}:run
```

Expected result: the application reaches the point where its normal infrastructure initializes. For Nacos applications, verify both config loading and service registration.

## Spring Cloud Alibaba 2025 And Nacos Config

For Spring Cloud Alibaba `2025.0.x`, prefer explicit Config Data imports for Nacos configuration:

```yaml
spring:
  config:
    import:
      - nacos:service-common.yml?group=JAVA&refreshEnabled=true
      - nacos:${spring.application.name}.yml?group=JAVA&refreshEnabled=true
```

Adjust the data IDs and groups to match the application. Do not copy private Nacos addresses, namespaces, accounts, or passwords into reusable documentation.

Keep the normal Nacos connection properties in application-owned configuration:

```yaml
spring:
  cloud:
    nacos:
      server-addr: ${NACOS_ADDRESS:127.0.0.1:8848}
      username: ${NACOS_USERNAME:nacos}
      password: ${NACOS_PASSWORD:nacos}
      discovery:
        namespace: ${NACOS_NAMESPACE:public}
      config:
        namespace: ${NACOS_NAMESPACE:public}
        group: DEFAULT_GROUP
        file-extension: yml
```

## OpenFeign 4.3 Attribute Resolution

Spring Cloud OpenFeign `4.3.x` resolves `@FeignClient` attributes eagerly by default. This is useful for startup-time determinism and AOT/native image support, but existing applications often keep Feign URLs in remote configuration. If those placeholders are not available during early startup, the application can fail while validating a URL such as `http://${feign.client.config.example.url}`.

Use this compatibility switch while migrating:

```yaml
spring:
  cloud:
    openfeign:
      lazy-attributes-resolution: true
```

Treat this as a migration compatibility setting. Over time, prefer configuration that is available before Feign clients are registered.

## MyBatis-Plus 3.5.16 Pagination Dependency

MyBatis-Plus `3.5.16` keeps `PaginationInnerInterceptor` in `mybatis-plus-jsqlparser`. Applications or platform libraries that create a `PaginationInnerInterceptor` need this dependency:

```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-jsqlparser</artifactId>
</dependency>
```

Use the same MyBatis-Plus version as the rest of the platform. If the application imports `jfoundry-dependencies`, the version is managed by the BOM.

## JobRunr 8 Annotation Package

JobRunr `8.x` moved `@Recurring` to:

```java
import org.jobrunr.jobs.annotations.Recurring;
```

Replace the old import:

```java
import org.jobrunr.spring.annotations.Recurring;
```

The annotation usage can usually stay unchanged.

## Nacos Client Shutdown Observation

Nacos Client `3.0.3` can emit shutdown-order warnings or exceptions during local `Ctrl-C`, even after service registration and deregistration have completed. Treat this as a compatibility observation unless it prevents normal graceful shutdown in the target environment.

## What Must Stay In The Application

Do not move these details into `jfoundry`:

- private Maven profile names and repository URLs
- Nacos server addresses, usernames, passwords, namespaces, and group conventions
- business service names
- database URLs and credentials
- message broker addresses
- company-specific SDKs
- migration strategy for a legacy in-house framework

`jfoundry` should document the integration shape and compatibility traps. Applications own their environment and business context.

## When To Add Runtime Starters

Add runtime starters only after the platform baseline is stable:

```xml
<dependency>
    <groupId>org.jfoundry</groupId>
    <artifactId>jfoundry-spring-boot-starter</artifactId>
</dependency>
```

For MyBatis-Plus business persistence pilots, add:

```xml
<dependency>
    <groupId>org.jfoundry</groupId>
    <artifactId>jfoundry-mybatis-plus-spring-boot-starter</artifactId>
</dependency>
```

Outbox, Inbox, Messaging, broker adapters, and JobRunr dispatchers should be added only when a concrete business scenario needs them.
