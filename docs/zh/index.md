# jfoundry 中文文档

本文档按概念组织，而不是按模块堆砌。业务接入先看集成指南，架构判断先看框架语义，维护者再看框架边界和发布文档。

## 框架语义

- [架构风格指南](framework/architecture-styles.md)
- [ArchUnit 架构规则](framework/archunit-rules.md)
- [框架边界设计](framework/framework-boundaries.md)

## 建模约定

- [值对象规范](modeling/value-object.md)
- [Repository 与读侧端口迁移指南](modeling/repository-vs-read-ports.md)

## 技术集成

- [业务项目接入指南](integration/getting-started-for-business-projects.md)
- [Spring Boot 自动配置总览](integration/spring-boot-autoconfiguration.md)
- [应用事务与分布式锁](integration/application-transactions-and-locks.md)
- [持久化 DataConverter 与 MapStruct 使用指南](integration/persistence-data-converters.md)
- [Transactional Outbox 事务性发件箱](integration/transactional-outbox.md)

## 发布与兼容

发布、兼容矩阵和 Maven Central 说明是维护者文档，当前只维护一份：

- [Compatibility Matrix](../release/compatibility.md)
- [Maven Central Publishing](../release/maven-central.md)
