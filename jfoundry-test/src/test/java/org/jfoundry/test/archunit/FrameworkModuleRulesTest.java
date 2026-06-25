package org.jfoundry.test.archunit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FrameworkModuleRulesTest {

    @Test
    void allFrameworkModuleRulesAreNonNull() {
        assertThat(FrameworkModuleRules.domain_must_not_depend_on_outer_layers).isNotNull();
        assertThat(FrameworkModuleRules.application_must_not_depend_on_outer_layers).isNotNull();
        assertThat(FrameworkModuleRules.application_packages_should_be_hexagonal_application).isNotNull();
        assertThat(FrameworkModuleRules.infrastructure_adapter_packages_should_be_hexagonal_adapters).isNotNull();
        assertThat(FrameworkModuleRules.infrastructure_must_not_depend_on_spring_autoconfigure).isNotNull();
        assertThat(FrameworkModuleRules.application_store_ports_should_be_secondary_ports).isNotNull();
        assertThat(FrameworkModuleRules.message_sender_should_be_secondary_port).isNotNull();
        assertThat(FrameworkModuleRules.payload_serializer_should_be_secondary_port).isNotNull();
        assertThat(FrameworkModuleRules.infrastructure_mybatis_message_stores_should_be_secondary_adapters).isNotNull();
        assertThat(FrameworkModuleRules.infrastructure_adapter_packages_should_be_secondary_adapters).isNotNull();
        assertThat(FrameworkModuleRules.kafka_message_sender_should_be_secondary_adapter).isNotNull();
        assertThat(FrameworkModuleRules.jackson_payload_serializer_should_be_secondary_adapter).isNotNull();
    }
}
