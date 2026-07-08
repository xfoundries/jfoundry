package org.jfoundry.autoconfigure.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/// P2-3 regression: {@code jmolecules-jackson} already ships {@code JMoleculesJacksonAutoConfiguration}
/// registered through its {@code META-INF/spring/...AutoConfiguration.imports}. A previously copied
/// {@code JfoundryJacksonAutoConfiguration} caused duplicate same-name beans.
/// <p>
/// This test verifies that after removing {@code JfoundryJacksonAutoConfiguration}, the jMolecules
/// module is still registered with ObjectMapper through upstream auto-configuration.
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
                .as("jMolecules Jackson upstream auto-configuration must register JMoleculesModule")
                .anyMatch(id -> id.toString().contains("jmolecules"));
    }
}
