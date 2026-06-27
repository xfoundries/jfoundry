package org.jfoundry.integration.rabbitmq;

import org.jfoundry.application.messaging.SendResult;
import org.jfoundry.application.outbox.DefaultOutboxDispatchService;
import org.jfoundry.infrastructure.messaging.rabbitmq.RabbitMqMessageSender;
import org.jfoundry.infrastructure.outbox.mybatis.MybatisPlusOutboxMessageStore;
import org.jfoundry.infrastructure.outbox.mybatis.OutboxData;
import org.jfoundry.infrastructure.outbox.mybatis.OutboxMapper;
import org.jfoundry.integration.support.OutboxInboxDatabaseConfig;
import org.jfoundry.integration.support.OutboxMessages;
import org.jfoundry.integration.support.SqlScripts;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(classes = OutboxInboxDatabaseConfig.class)
class RabbitMqOutboxDispatchIT {

    private static final String EXCHANGE = "jfoundry.integration.outbox";
    private static final String ROUTING_KEY = "order.created";
    private static final String QUEUE = "jfoundry.integration.outbox.order.created";

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("jfoundry")
            .withUsername("jfoundry")
            .withPassword("jfoundry");

    @Container
    static final RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @Autowired
    private MybatisPlusOutboxMessageStore store;

    @Autowired
    private OutboxMapper mapper;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
    }

    @BeforeAll
    static void createSchema(@Autowired DataSource dataSource) {
        SqlScripts.run(dataSource, "db/migration/V20260617__create_outbox_event.sql");
    }

    @BeforeEach
    void cleanDb() {
        mapper.delete(null);
        RabbitAdmin admin = new RabbitAdmin(connectionFactory());
        DirectExchange exchange = new DirectExchange(EXCHANGE, true, false);
        Queue queue = new Queue(QUEUE, true, false, false);
        admin.declareExchange(exchange);
        admin.declareQueue(queue);
        admin.declareBinding(BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY));
        rabbitTemplate().receiveAndConvert(QUEUE);
    }

    @Test
    void dispatchPublishesRabbitMqMessageAndMarksOutboxPublished() {
        store.append(OutboxMessages.pending(
                "evt-rabbit-1",
                EXCHANGE,
                ROUTING_KEY,
                "{\"event\":\"created\"}"));

        new DefaultOutboxDispatchService(
                store,
                new RabbitMqMessageSender(rabbitTemplate()),
                3,
                retry -> Duration.ofMillis(10),
                "it-pod").dispatch(10);

        Object payload = rabbitTemplate().receiveAndConvert(QUEUE, 10_000);

        assertThat(payload).isEqualTo("{\"event\":\"created\"}");
        OutboxData data = mapper.selectById("evt-rabbit-1");
        assertThat(data.getStatus()).isEqualTo("PUBLISHED");
        assertThat(data.getClaimToken()).isNull();
        assertThat(data.getClaimedAt()).isNull();
        assertThat(data.getClaimedBy()).isNull();
    }

    @Test
    void dispatchFailureMarksOutboxFailedAndKeepsRetryMetadata() {
        store.append(OutboxMessages.pending(
                "evt-rabbit-fail",
                EXCHANGE,
                ROUTING_KEY,
                "{\"event\":\"created\"}"));

        new DefaultOutboxDispatchService(
                store,
                (topic, key, payload) -> SendResult.fail("broker down"),
                3,
                retry -> Duration.ofMillis(10),
                "it-pod").dispatch(10);

        OutboxData data = mapper.selectById("evt-rabbit-fail");
        assertThat(data.getStatus()).isEqualTo("FAILED");
        assertThat(data.getRetryCount()).isEqualTo(1);
        assertThat(data.getErrorMessage()).contains("broker down");
        assertThat(data.getNextRetryAt()).isNotNull();
        assertThat(data.getClaimToken()).isNull();
        assertThat(data.getClaimedAt()).isNull();
        assertThat(data.getClaimedBy()).isNull();
    }

    private static CachingConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(
                rabbitmq.getHost(), rabbitmq.getAmqpPort());
        connectionFactory.setUsername(rabbitmq.getAdminUsername());
        connectionFactory.setPassword(rabbitmq.getAdminPassword());
        return connectionFactory;
    }

    private static RabbitTemplate rabbitTemplate() {
        return new RabbitTemplate(connectionFactory());
    }
}
