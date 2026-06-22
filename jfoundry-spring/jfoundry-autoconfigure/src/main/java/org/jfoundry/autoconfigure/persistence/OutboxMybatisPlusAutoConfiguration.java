package org.jfoundry.autoconfigure.persistence;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TableNameHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.DynamicTableNameInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.jfoundry.infrastructure.messaging.outbox.OutboxRepository;
import org.jfoundry.infrastructure.messaging.mybatis.outbox.MybatisPlusOutboxRepository;
import org.jfoundry.infrastructure.messaging.mybatis.outbox.OutboxMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/// MybatisPlus 默认 Bean 自动配置：
/// <ul>
///   <li>@MapperScan 显式扫描 OutboxMapper 包，由 ImportBeanDefinitionRegistrar 在 ConfigurationClassParser
///       阶段注册 bean 定义，供下游 AutoConfiguration 在 @Autowired 注入时解析。</li>
///   <li>未注册任何 MybatisPlusInterceptor 时提供仅含 PaginationInnerInterceptor 的默认实例。</li>
///   <li>注册 MybatisPlusOutboxRepository 作为 OutboxRepository 默认实现。</li>
///   <li>P2-2: 在 MybatisPlusInterceptor 中追加 {@link DynamicTableNameInnerInterceptor}，
///       把 OutboxData 的逻辑表名 {@code ddd_outbox_event} 重写为业务配置的
///       {@code jfoundry.outbox.table-name}（默认值不变，向后兼容）。</li>
/// </ul>
/// <p>
/// 注：不在 mybatisPlusOutboxRepository 上加 @ConditionalOnBean(OutboxMapper.class) —— 因为
/// MapperScannerConfigurer 是 BeanDefinitionRegistryPostProcessor，注册时机晚于 @ConditionalOnBean
/// 评估。OutboxMapper 通过构造器参数 @Autowired 注入，若 mapper 缺失会在 bean 创建时明确报错。
@AutoConfiguration
@MapperScan(basePackages = "org.jfoundry.infrastructure.messaging.mybatis.outbox")
@EnableConfigurationProperties(JfoundryOutboxProperties.class)
public class OutboxMybatisPlusAutoConfiguration {

    /// 逻辑表名 — 与 {@code OutboxData.@TableName("ddd_outbox_event")} 对齐。
    /// <p>
    /// 即使业务通过 {@code jfoundry.outbox.table-name} 覆盖了物理表名，OutboxData 上的
    /// {@code @TableName} 仍作为 MP 解析阶段的 logical name；{@link DynamicTableNameInnerInterceptor}
    /// 在 SQL 序列化前把这个 logical name 替换为配置值。固定的常量便于 handler 精确匹配，
    /// 不会误伤业务其它表。
    private static final String OUTBOX_LOGICAL_TABLE = "ddd_outbox_event";

    @Bean
    @ConditionalOnMissingBean(MybatisPlusInterceptor.class)
    public MybatisPlusInterceptor mybatisPlusInterceptor(JfoundryOutboxProperties outboxProperties) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // P2-2: 表名重写必须先于 pagination —— pagination 在 rewritten SQL 上加 LIMIT。
        interceptor.addInnerInterceptor(outboxTableNameInterceptor(outboxProperties));
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return interceptor;
    }

    @Bean
    @ConditionalOnMissingBean(OutboxRepository.class)
    public OutboxRepository mybatisPlusOutboxRepository(
            OutboxMapper outboxMapper,
            MybatisPlusInterceptor mybatisPlusInterceptor) {
        return new MybatisPlusOutboxRepository(outboxMapper, mybatisPlusInterceptor);
    }

    /// P2-2: 构造动态表名 inner interceptor，把 OutboxData 的逻辑表名重写为
    /// {@code jfoundry.outbox.table-name}。handler 只匹配 {@code ddd_outbox_event}，
    /// 业务其它表不受影响。
    /// <p>
    /// 默认配置下 ({@code table-name=ddd_outbox_event}) handler 返回值与入参一致，
    /// 等价于 no-op —— 保持向后兼容。若业务将 {@code table-name} 设为空串或与默认相同，
    /// 该 handler 仍只对 outbox logical name 生效。
    private DynamicTableNameInnerInterceptor outboxTableNameInterceptor(JfoundryOutboxProperties properties) {
        String configured = properties.getTableName();
        // TableNameHandler.dynamicTableName(sqlStatement, tableName) — first arg is the
        // full SQL, second is the parsed table-name token. Compare on the table-name arg.
        TableNameHandler handler = (sqlStatement, tableName) ->
                OUTBOX_LOGICAL_TABLE.equals(tableName) ? configured : tableName;
        return new DynamicTableNameInnerInterceptor(handler);
    }
}
