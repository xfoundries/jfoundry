package org.jfoundry.autoconfigure.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/// P3-3: jmolecules Jackson module must be auto-registered so that ValueObject and
/// Identifier types serialize/deserialize as single values (not wrapped JSON objects).
/// <p>
/// The test pulls spring-web onto the test classpath via this module's test-scope
/// spring-web dependency so that Spring Boot's JacksonAutoConfiguration builds the
/// default ObjectMapper via Jackson2ObjectMapperBuilder, which applies all registered
/// Module beans (including JMoleculesModule). This mirrors a real Spring Boot app
/// where spring-web is always present (via spring-boot-starter-web or -webflux).
@SpringBootTest(classes = JfoundryJacksonAutoConfigurationTest.TestApp.class)
class JfoundryJacksonAutoConfigurationTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApp {
    }

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void jmoleculesModuleIsRegistered() {
        assertThat(objectMapper.getRegisteredModuleIds())
                .as("JMolecules module must be registered with ObjectMapper")
                .contains("jmolecules-module");
    }

    @Test
    void moduleIsResolvableAsBean() {
        // The autoconfig registers a Module bean; verify it's the jmolecules one
        // by checking module type name via ObjectMapper's registered modules.
        boolean hasJmolecules = objectMapper.getRegisteredModuleIds().stream()
                .anyMatch(id -> id.toString().contains("jmolecules"));
        assertThat(hasJmolecules).isTrue();
    }
}
