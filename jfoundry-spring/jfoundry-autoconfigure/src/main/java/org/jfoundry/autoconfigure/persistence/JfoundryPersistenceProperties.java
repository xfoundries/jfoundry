package org.jfoundry.autoconfigure.persistence;

import com.baomidou.mybatisplus.annotation.DbType;
import org.springframework.boot.context.properties.ConfigurationProperties;

/// jfoundry 持久化配置。
/// <p>
/// Prefix: {@code jfoundry.persistence}
/// <p>
/// P2-3: 通过 {@link #dbType} 业务侧可显式声明数据库方言；未配置（{@code null}）时
/// 由 {@link DbTypeResolver} 从 {@link javax.sql.DataSource} metadata 自动推断。
/// <p>
/// 适用场景：
/// <ul>
///   <li>HikariCP / Druid 等连接池包装 DataSource，导致 MP 默认自动检测失效；</li>
///   <li>生产环境使用达梦 (DM) 等非主流数据库，需强制指定；</li>
///   <li>测试或影子库场景下需要切换方言。</li>
/// </ul>
@ConfigurationProperties(prefix = "jfoundry.persistence")
public class JfoundryPersistenceProperties {

    /// 显式指定 MyBatis-Plus 数据库方言。{@code null} 表示自动从 DataSource metadata 推断。
    /// <p>
    /// 配置后由 {@link DbTypeResolver#resolve} 优先返回此值，覆盖自动检测结果。
    private DbType dbType;

    public DbType getDbType() { return dbType; }
    public void setDbType(DbType dbType) { this.dbType = dbType; }
}
