package org.jfoundry.infrastructure.messaging.rabbitmq.quarkus;

import io.quarkus.arc.DefaultBean;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQOptions;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jfoundry.application.messaging.MessageSender;
import org.jfoundry.application.messaging.SendResult;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/// Quarkus RabbitMQ-backed {@link MessageSender}.
@ApplicationScoped
@DefaultBean
public class QuarkusRabbitMqMessageSender implements MessageSender {

    private final RabbitMQClient client;
    private final Duration sendTimeout;

    @Inject
    public QuarkusRabbitMqMessageSender(
            Vertx vertx,
            @ConfigProperty(name = "jfoundry.messaging.rabbitmq.host", defaultValue = "localhost") String host,
            @ConfigProperty(name = "jfoundry.messaging.rabbitmq.port", defaultValue = "5672") int port,
            @ConfigProperty(name = "jfoundry.messaging.rabbitmq.username", defaultValue = "guest") String username,
            @ConfigProperty(name = "jfoundry.messaging.rabbitmq.password", defaultValue = "guest") String password,
            @ConfigProperty(name = "jfoundry.messaging.rabbitmq.virtual-host", defaultValue = "/") String virtualHost,
            @ConfigProperty(name = "jfoundry.messaging.rabbitmq.send-timeout", defaultValue = "10s") Duration sendTimeout) {
        this(RabbitMQClient.create(vertx, new RabbitMQOptions()
                .setHost(host)
                .setPort(port)
                .setUser(username)
                .setPassword(password)
                .setVirtualHost(virtualHost)
                .setAutomaticRecoveryEnabled(true)), sendTimeout);
    }

    QuarkusRabbitMqMessageSender(RabbitMQClient client, Duration sendTimeout) {
        this.client = client;
        this.sendTimeout = sendTimeout;
    }

    @Override
    public SendResult send(String topic, String payloadKey, String payload) {
        try {
            if (!client.isConnected()) {
                await(client.start());
            }
            await(client.basicPublish(topic, payloadKey == null ? "" : payloadKey, Buffer.buffer(payload)));
            return SendResult.ok();
        } catch (Exception exception) {
            Throwable cause = exception.getCause() != null ? exception.getCause() : exception;
            return SendResult.fail(cause.getMessage());
        }
    }

    @PreDestroy
    void stop() {
        client.stop();
    }

    private void await(Future<Void> future) throws Exception {
        future.toCompletionStage().toCompletableFuture().get(sendTimeout.toMillis(), TimeUnit.MILLISECONDS);
    }
}
