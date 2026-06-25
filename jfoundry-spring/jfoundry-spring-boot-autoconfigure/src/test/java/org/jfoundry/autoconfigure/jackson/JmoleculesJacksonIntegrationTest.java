package org.jfoundry.autoconfigure.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/// P2-3 regression: jmolecules-jackson 自带 {@code JMoleculesJacksonAutoConfiguration}
/// （通过其 {@code META-INF/spring/...AutoConfiguration.imports} 自注册），
/// 我们之前复制粘贴了一份 {@code JfoundryJacksonAutoConfiguration}，导致同名 bean 重复。
/// <p>
/// 本测试验证：删除 {@code JfoundryJacksonAutoConfiguration} 后，jmolecules module
/// 仍然能通过上游 autoconfig 自动注册到 ObjectMapper。
@SpringBootTest(classes = JmoleculesJacksonIntegrationTest.TestApp.class)
class JmoleculesJacksonIntegrationTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApp {
    }

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void jmoleculesModuleIsRegisteredByUpstreamAutoConfiguration() {
        assertThat(objectMapper.getRegisteredModuleIds())
                .as("jmolecules-jackson 上游 autoconfig 必须注册 JMoleculesModule")
                .anyMatch(id -> id.toString().contains("jmolecules"));
    }
}
