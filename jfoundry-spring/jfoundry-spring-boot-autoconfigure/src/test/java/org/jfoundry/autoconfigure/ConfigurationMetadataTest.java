package org.jfoundry.autoconfigure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigurationMetadataTest {

    @Test
    void documentsAllOutboxPropertiesInGeneratedMetadata() throws IOException {
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("META-INF/spring-configuration-metadata.json")) {
            assertThat(input).as("Spring Boot configuration metadata").isNotNull();

            JsonNode properties = new ObjectMapper().readTree(input).path("properties");
            List<String> undocumented = new ArrayList<>();
            for (JsonNode property : properties) {
                String name = property.path("name").asText();
                if (name.startsWith("jfoundry.outbox.") && property.path("description").asText().isBlank()) {
                    undocumented.add(name);
                }
            }

            assertThat(undocumented).isEmpty();
        }
    }
}
