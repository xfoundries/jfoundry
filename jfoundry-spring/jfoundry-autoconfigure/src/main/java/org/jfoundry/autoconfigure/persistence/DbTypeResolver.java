package org.jfoundry.autoconfigure.persistence;

import com.baomidou.mybatisplus.annotation.DbType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/// 解析 MyBatis-Plus {@link DbType}：显式配置优先，否则从 {@link DataSource} metadata 推断。
/// <p>
/// P2-3: 修复 HikariCP / Druid 等连接池包装 DataSource 导致 MP 默认自动检测失效
/// 的问题。业务侧通过 {@code jfoundry.persistence.db-type} 显式指定时优先使用该值；
/// 未配置时调用 {@link DataSource#getConnection()} 读取 {@code DatabaseMetaData} 的
/// {@code databaseProductName} 进行映射。
/// <p>
/// 映射规则按产品名子串匹配（大小写不敏感），未知产品回退为 {@link DbType#MYSQL}。
public class DbTypeResolver {

    private static final Logger log = LoggerFactory.getLogger(DbTypeResolver.class);

    /// 返回显式配置的 {@link DbType}，未配置时返回 {@code null}（由调用方决定回退策略）。
    /// <p>
    /// 适用于启动阶段不希望打开 Connection 的场景（例如 {@link OutboxMybatisPlusAutoConfiguration}
    /// 在未显式配置时直接回退到 MP 默认自动检测，避免拖慢 ApplicationContext 启动）。
    public DbType resolveExplicit(JfoundryPersistenceProperties props) {
        if (props != null && props.getDbType() != null) {
            log.debug("DbType from explicit config: {}", props.getDbType());
            return props.getDbType();
        }
        return null;
    }

    /// 显式配置优先，否则从 {@link DataSource} metadata 自动推断。
    /// <p>
    /// 调用此方法会打开一个 Connection 读取 {@code DatabaseMetaData}。在 ApplicationContext
    /// 启动阶段调用可能触发 DataSource 提前初始化，影响 bean 创建时序，建议仅在
    /// 测试或明确需要自动检测的场景使用。AutoConfig 推荐调用 {@link #resolveExplicit}。
    public DbType resolve(JfoundryPersistenceProperties props, DataSource dataSource) {
        DbType explicit = resolveExplicit(props);
        return explicit != null ? explicit : autoDetect(dataSource);
    }

    public DbType autoDetect(DataSource dataSource) {
        if (dataSource == null) {
            log.warn("DataSource is null, falling back to MYSQL");
            return DbType.MYSQL;
        }
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
