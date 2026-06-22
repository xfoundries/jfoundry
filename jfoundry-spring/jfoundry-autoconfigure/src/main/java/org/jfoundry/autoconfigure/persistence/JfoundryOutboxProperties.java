package org.jfoundry.autoconfigure.persistence;

import org.springframework.boot.context.properties.ConfigurationProperties;

/// Outbox 配置。
/// <p>
/// Prefix: {@code jfoundry.outbox}
/// <p>
/// P2-2: 通过 {@link #tableName} 业务侧可覆盖 OutboxData 实体对应的物理表名；
/// {@link OutboxMybatisPlusAutoConfiguration} 会注册
/// {@code DynamicTableNameInnerInterceptor} 把框架内部的逻辑表名
/// {@code ddd_outbox_event} 在运行时重写为业务配置的表名。
@ConfigurationProperties(prefix = "jfoundry.outbox")
public class JfoundryOutboxProperties {

    /// OutboxData 实体对应的物理表名。默认 {@code ddd_outbox_event}（向后兼容）。
    /// <p>
    /// 配置后由 {@code DynamicTableNameInnerInterceptor} 在 SQL 解析阶段进行表名重写，
    /// 业务侧需自行通过 Flyway/Liquibase 或手动 DDL 创建对应表，schema 需与
    /// {@code ddd_outbox_event} 一致（event_id / topic / payload_* / status / retry_count /
    /// error_message / occurred_at / last_attempt_at / next_retry_at / created_at /
    /// updated_at / claimed_at / claimed_by）。
    private String tableName = "ddd_outbox_event";

    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
}
