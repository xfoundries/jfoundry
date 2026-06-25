package org.jfoundry.test.archunit;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "org.jfoundry", importOptions = ImportOption.DoNotIncludeTests.class)
class FrameworkArchitectureSelfTest {

    @ArchTest
    static final ArchRule domain_must_not_depend_on_outer_layers =
            FrameworkModuleRules.domain_must_not_depend_on_outer_layers;

    @ArchTest
    static final ArchRule application_must_not_depend_on_outer_layers =
            FrameworkModuleRules.application_must_not_depend_on_outer_layers;

    @ArchTest
    static final ArchRule domain_packages_should_be_domain_layer =
            FrameworkModuleRules.domain_packages_should_be_domain_layer;

    @ArchTest
    static final ArchRule infrastructure_packages_should_be_infrastructure_layer =
            FrameworkModuleRules.infrastructure_packages_should_be_infrastructure_layer;

    @ArchTest
    static final ArchRule infrastructure_must_not_depend_on_spring_autoconfigure =
            FrameworkModuleRules.infrastructure_must_not_depend_on_spring_autoconfigure;

    @ArchTest
    static final ArchRule application_store_ports_should_be_secondary_ports =
            FrameworkModuleRules.application_store_ports_should_be_secondary_ports;

    @ArchTest
    static final ArchRule message_sender_should_be_secondary_port =
            FrameworkModuleRules.message_sender_should_be_secondary_port;

    @ArchTest
    static final ArchRule payload_serializer_should_be_secondary_port =
            FrameworkModuleRules.payload_serializer_should_be_secondary_port;

    @ArchTest
    static final ArchRule infrastructure_mybatis_message_stores_should_be_secondary_adapters =
            FrameworkModuleRules.infrastructure_mybatis_message_stores_should_be_secondary_adapters;

    @ArchTest
    static final ArchRule infrastructure_adapter_packages_should_be_secondary_adapters =
            FrameworkModuleRules.infrastructure_adapter_packages_should_be_secondary_adapters;

    @ArchTest
    static final ArchRule kafka_message_sender_should_be_secondary_adapter =
            FrameworkModuleRules.kafka_message_sender_should_be_secondary_adapter;

    @ArchTest
    static final ArchRule jackson_payload_serializer_should_be_secondary_adapter =
            FrameworkModuleRules.jackson_payload_serializer_should_be_secondary_adapter;
}
