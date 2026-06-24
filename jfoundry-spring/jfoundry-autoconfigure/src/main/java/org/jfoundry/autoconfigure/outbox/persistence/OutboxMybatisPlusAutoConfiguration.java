package org.jfoundry.autoconfigure.outbox.persistence;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.jfoundry.autoconfigure.outbox.JfoundryOutboxProperties;
import org.jfoundry.autoconfigure.persistence.DbTypeResolver;
import org.jfoundry.autoconfigure.persistence.JfoundryPersistenceProperties;
import org.jfoundry.application.outbox.OutboxRepository;
import org.jfoundry.infrastructure.outbox.mybatis.MybatisPlusOutboxRepository;
import org.jfoundry.infrastructure.outbox.mybatis.OutboxMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/// MybatisPlus 默认 Bean 自动配置：
/// <ul>
///   <li>@MapperScan 显式扫描 OutboxMapper 包，由 ImportBeanDefinitionRegistrar 在 ConfigurationClassParser
///       阶段注册 bean 定义，供下游 AutoConfiguration 在 @Autowired 注入时解析。</li>
///   <li>未注册任何 MybatisPlusInterceptor 时提供仅含 PaginationInnerInterceptor 的默认实例。</li>
///   <li>注册 MybatisPlusOutboxRepository 作为 OutboxRepository 默认实现。</li>
///   <li>P2-2: 在 MybatisPlusInterceptor 中追加 outbox 动态表名 inner interceptor，
///       把 OutboxData 的逻辑表名 {@code jfoundry_outbox_event} 重写为业务配置的
///       {@code jfoundry.outbox.table-name}。</li>
///   <li>P2-3: 当 {@code jfoundry.persistence.db-type} 显式配置时，用该值构造
///       {@link PaginationInnerInterceptor}；未配置时回退到 MP 默认行为（由 MP 在首次
///       分页查询时通过 {@code JdbcUtils.extractDatabaseMetaData} 自动检测）。
///       显式配置解决 HikariCP / Druid 等连接池包装 DataSource 导致 MP 默认检测失效
///       的问题。不在未配置时主动打开 Connection 检测，避免在 ApplicationContext
///       启动阶段触发 DataSource 提前初始化（影响其它依赖启动时序的测试）。</li>
/// </ul>
/// <p>
/// 注：不在 mybatisPlusOutboxRepository 上加 @ConditionalOnBean(OutboxMapper.class) —— 因为
/// MapperScannerConfigurer 是 BeanDefinitionRegistryPostProcessor，注册时机晚于 @ConditionalOnBean
/// 评估。OutboxMapper 通过构造器参数 @Autowired 注入，若 mapper 缺失会在 bean 创建时明确报错。
@AutoConfiguration
@MapperScan(basePackages = "org.jfoundry.infrastructure.outbox.mybatis")
@EnableConfigurationProperties({JfoundryOutboxProperties.class, JfoundryPersistenceProperties.class})
public class OutboxMybatisPlusAutoConfiguration {

    @Bean
    public OutboxTableNameCustomizer outboxTableNameCustomizer(JfoundryOutboxProperties properties) {
        return new OutboxTableNameCustomizer(properties);
    }

    @Bean
    @ConditionalOnMissingBean(MybatisPlusInterceptor.class)
    public MybatisPlusInterceptor mybatisPlusInterceptor(
            OutboxTableNameCustomizer outboxTableNameCustomizer,
            JfoundryPersistenceProperties persistenceProperties) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // P2-2: 表名重写必须先于 pagination —— pagination 在 rewritten SQL 上加 LIMIT。
        outboxTableNameCustomizer.customize(interceptor);
        interceptor.addInnerInterceptor(paginationInnerInterceptor(persistenceProperties));
        return interceptor;
    }

    @Bean
    public static BeanPostProcessor outboxTableNameInterceptorPostProcessor(
            Environment environment) {
        JfoundryOutboxProperties properties = new JfoundryOutboxProperties();
        properties.setTableName(environment.getProperty(
                "jfoundry.outbox.table-name",
                OutboxTableNameCustomizer.OUTBOX_LOGICAL_TABLE));
        OutboxTableNameCustomizer outboxTableNameCustomizer = new OutboxTableNameCustomizer(properties);
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof MybatisPlusInterceptor interceptor) {
                    outboxTableNameCustomizer.customize(interceptor);
                }
                return bean;
            }
        };
    }

    /// P2-3: 当业务通过 {@code jfoundry.persistence.db-type} 显式配置方言时，用该值构造
    /// {@link PaginationInnerInterceptor}，避免 MP 默认自动检测被 HikariCP / Druid 等连接池
    /// 包装干扰。未显式配置时回退到 MP 默认行为（no-arg 构造，MP 在首次分页查询时通过
    /// {@code JdbcUtils.extractDatabaseMetaData} 自动检测），保持向后兼容。
    /// <p>
    /// 这里仅查询显式配置；不调用 {@link DbTypeResolver#autoDetect} 是为了避免在 bean
    /// 创建阶段打开 Connection 拖慢 ApplicationContext 启动。业务侧若需要强制覆盖自动检测
    /// 但又不方便配置固定值，可自行注入 {@link DbTypeResolver} 并在首次查询前调用
    /// {@code autoDetect}。
    private PaginationInnerInterceptor paginationInnerInterceptor(
            JfoundryPersistenceProperties persistenceProperties) {
        DbType dbType = new DbTypeResolver().resolveExplicit(persistenceProperties);
        return dbType != null
                ? new PaginationInnerInterceptor(dbType)
                : new PaginationInnerInterceptor();
    }

    @Bean
    @ConditionalOnMissingBean(OutboxRepository.class)
    public OutboxRepository mybatisPlusOutboxRepository(
            OutboxMapper outboxMapper,
            MybatisPlusInterceptor mybatisPlusInterceptor) {
        return new MybatisPlusOutboxRepository(outboxMapper, mybatisPlusInterceptor);
    }
}
