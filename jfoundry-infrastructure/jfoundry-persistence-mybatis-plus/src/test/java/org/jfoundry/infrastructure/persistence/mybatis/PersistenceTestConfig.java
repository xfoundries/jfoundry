package org.jfoundry.infrastructure.persistence.mybatis;

import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import org.jfoundry.domain.event.DomainEventPublisher;
import org.jfoundry.infrastructure.persistence.mybatis.support.TestOrderDataConverter;
import org.jfoundry.infrastructure.persistence.mybatis.support.TestOrderIdTypeHandler;
import org.jfoundry.infrastructure.persistence.mybatis.support.TestOrderMapper;
import org.jfoundry.infrastructure.persistence.mybatis.support.TestOrderRepository;
import org.jfoundry.test.DomainEventCapture;
import org.jfoundry.test.DomainEventPublisherStub;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;

/// 集成测试 Spring 配置。
/// <p>
/// 镜像 jfoundry-outbox-mybatis-plus 的 OutboxPersistenceTestConfig 模式:
/// H2 embedded + 手写 @SpringBootConfiguration + @MapperScan。
@SpringBootConfiguration
@EnableAutoConfiguration
@MapperScan(basePackageClasses = TestOrderMapper.class)
class PersistenceTestConfig {

    @Bean
    DomainEventCapture domainEventCapture() {
        return new DomainEventCapture();
    }

    @Bean
    DomainEventPublisher domainEventPublisher(DomainEventCapture capture) {
        return new DomainEventPublisherStub(capture);
    }

    @Bean
    DataSource dataSource() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .setName("jfoundry-persistence-test")
                .addScript("classpath:test_order.sql")
                .build();
    }

    /// 注册 TestOrderId TypeHandler。
    /// MyBatis-Plus 的 @TableId 注解不支持 typeHandler 属性,只能通过全局 TypeHandlerRegistry 注册。
    @Bean
    ConfigurationCustomizer mybatisConfigurationCustomizer() {
        return configuration -> configuration
                .getTypeHandlerRegistry()
                .register(new TestOrderIdTypeHandler());
    }

    @Bean
    TestOrderRepository testOrderRepository(TestOrderMapper mapper,
                                             DomainEventPublisher eventPublisher,
                                             TestOrderDataConverter converter) {
        return new TestOrderRepository(mapper, eventPublisher, converter);
    }

    @Bean
    TestOrderDataConverter testOrderDataConverter() {
        return new TestOrderDataConverter();
    }
}
