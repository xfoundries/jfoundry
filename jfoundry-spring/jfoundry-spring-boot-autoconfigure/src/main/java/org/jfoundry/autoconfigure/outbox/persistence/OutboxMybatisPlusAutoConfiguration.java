package org.jfoundry.autoconfigure.outbox.persistence;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.jfoundry.autoconfigure.outbox.JfoundryOutboxProperties;
import org.jfoundry.application.outbox.OutboxMessageStore;
import org.jfoundry.infrastructure.outbox.mybatis.MybatisPlusOutboxMessageStore;
import org.jfoundry.infrastructure.outbox.mybatis.OutboxMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/// MybatisPlus 默认 Bean 自动配置：
/// <ul>
///   <li>@MapperScan 显式扫描 OutboxMapper 包，由 ImportBeanDefinitionRegistrar 在 ConfigurationClassParser
///       阶段注册 bean 定义，供下游 AutoConfiguration 在 @Autowired 注入时解析。</li>
///   <li>未注册任何 MybatisPlusInterceptor 时提供仅含 PaginationInnerInterceptor 的默认实例。</li>
///   <li>注册 MybatisPlusOutboxMessageStore 作为 OutboxMessageStore 默认实现。</li>
///   <li>P2-2: 在 MybatisPlusInterceptor 中追加 outbox 动态表名 inner interceptor，
///       把 OutboxData 的逻辑表名 {@code jfoundry_outbox_event} 重写为业务配置的
///       {@code jfoundry.outbox.table-name}。</li>
///   <li>分页方言交给 MyBatis-Plus 默认自动检测，避免重复定义数据库类型配置。</li>
/// </ul>
/// <p>
/// 注：不在 mybatisPlusOutboxMessageStore 上加 @ConditionalOnBean(OutboxMapper.class) —— 因为
/// MapperScannerConfigurer 是 BeanDefinitionRegistryPostProcessor，注册时机晚于 @ConditionalOnBean
/// 评估。OutboxMapper 通过构造器参数 @Autowired 注入，若 mapper 缺失会在 bean 创建时明确报错。
@AutoConfiguration
@AutoConfigureAfter(name = "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration")
@MapperScan(basePackages = "org.jfoundry.infrastructure.outbox.mybatis")
@ConditionalOnClass({MybatisPlusInterceptor.class, MapperScan.class, MybatisPlusOutboxMessageStore.class})
@EnableConfigurationProperties(JfoundryOutboxProperties.class)
public class OutboxMybatisPlusAutoConfiguration {

    @Bean
    public OutboxTableNameCustomizer outboxTableNameCustomizer(JfoundryOutboxProperties properties) {
        return new OutboxTableNameCustomizer(properties);
    }

    @Bean
    @ConditionalOnMissingBean(MybatisPlusInterceptor.class)
    public MybatisPlusInterceptor mybatisPlusInterceptor(
            OutboxTableNameCustomizer outboxTableNameCustomizer) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // P2-2: 表名重写必须先于 pagination —— pagination 在 rewritten SQL 上加 LIMIT。
        outboxTableNameCustomizer.customize(interceptor);
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return interceptor;
    }

    @Bean
    public SmartInitializingSingleton outboxTableNameInterceptorInitializer(
            ObjectProvider<MybatisPlusInterceptor> interceptors,
            OutboxTableNameCustomizer outboxTableNameCustomizer) {
        return () -> interceptors.orderedStream()
                .forEach(outboxTableNameCustomizer::customize);
    }

    @Bean
    @ConditionalOnMissingBean(OutboxMessageStore.class)
    public OutboxMessageStore mybatisPlusOutboxMessageStore(
            OutboxMapper outboxMapper,
            MybatisPlusInterceptor mybatisPlusInterceptor) {
        return new MybatisPlusOutboxMessageStore(outboxMapper, mybatisPlusInterceptor);
    }
}
