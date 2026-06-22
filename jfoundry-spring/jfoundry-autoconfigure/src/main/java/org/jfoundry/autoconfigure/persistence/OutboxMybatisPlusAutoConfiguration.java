package org.jfoundry.autoconfigure.persistence;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.jfoundry.infrastructure.messaging.outbox.OutboxRepository;
import org.jfoundry.infrastructure.messaging.mybatis.outbox.MybatisPlusOutboxRepository;
import org.jfoundry.infrastructure.messaging.mybatis.outbox.OutboxMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/// MybatisPlus 默认 Bean 自动配置：
/// <ul>
///   <li>@MapperScan 显式扫描 OutboxMapper 包，由 ImportBeanDefinitionRegistrar 在 ConfigurationClassParser
///       阶段注册 bean 定义，供下游 AutoConfiguration 在 @Autowired 注入时解析。</li>
///   <li>未注册任何 MybatisPlusInterceptor 时提供仅含 PaginationInnerInterceptor 的默认实例。</li>
///   <li>注册 MybatisPlusOutboxRepository 作为 OutboxRepository 默认实现。</li>
/// </ul>
/// <p>
/// 注：不在 mybatisPlusOutboxRepository 上加 @ConditionalOnBean(OutboxMapper.class) —— 因为
/// MapperScannerConfigurer 是 BeanDefinitionRegistryPostProcessor，注册时机晚于 @ConditionalOnBean
/// 评估。OutboxMapper 通过构造器参数 @Autowired 注入，若 mapper 缺失会在 bean 创建时明确报错。
@AutoConfiguration
@MapperScan(basePackages = "org.jfoundry.infrastructure.messaging.mybatis.outbox")
public class OutboxMybatisPlusAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(MybatisPlusInterceptor.class)
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
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
}
