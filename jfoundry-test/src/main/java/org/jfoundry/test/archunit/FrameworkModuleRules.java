package org.jfoundry.test.archunit;

import com.tngtech.archunit.lang.ArchRule;
import org.jfoundry.architecture.hexagonal.SecondaryAdapter;
import org.jfoundry.architecture.hexagonal.SecondaryPort;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/// JFoundry framework module boundary rules.
public final class FrameworkModuleRules {

    private FrameworkModuleRules() {
    }

    public static final ArchRule domain_must_not_depend_on_outer_layers =
            noClasses()
                    .that().resideInAPackage("org.jfoundry.domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.jfoundry.application..",
                            "org.jfoundry.infrastructure..",
                            "org.jfoundry.autoconfigure..")
                    .allowEmptyShould(true)
                    .because("domain code must not depend on application, infrastructure, or autoconfigure modules");

    public static final ArchRule application_must_not_depend_on_outer_layers =
            noClasses()
                    .that().resideInAPackage("org.jfoundry.application..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.jfoundry.infrastructure..",
                            "org.jfoundry.autoconfigure..")
                    .allowEmptyShould(true)
                    .because("application code must not depend on infrastructure adapters or autoconfiguration");

    public static final ArchRule infrastructure_must_not_depend_on_spring_autoconfigure =
            noClasses()
                    .that().resideInAPackage("org.jfoundry.infrastructure..")
                    .should().dependOnClassesThat().resideInAnyPackage("org.jfoundry.autoconfigure..")
                    .allowEmptyShould(true)
                    .because("infrastructure adapters must stay independent from Spring Boot autoconfiguration");

    public static final ArchRule application_store_ports_should_be_secondary_ports =
            classes()
                    .that().resideInAPackage("org.jfoundry.application..")
                    .and().haveSimpleNameEndingWith("Store")
                    .should().beAnnotatedWith(SecondaryPort.class)
                    .allowEmptyShould(true)
                    .because("application Store abstractions are driven by infrastructure and must be secondary ports");

    public static final ArchRule message_sender_should_be_secondary_port =
            classes()
                    .that().haveFullyQualifiedName("org.jfoundry.application.messaging.MessageSender")
                    .should().beAnnotatedWith(SecondaryPort.class)
                    .allowEmptyShould(true)
                    .because("MessageSender is an outbound messaging port");

    public static final ArchRule payload_serializer_should_be_secondary_port =
            classes()
                    .that().haveFullyQualifiedName("org.jfoundry.application.messaging.PayloadSerializer")
                    .should().beAnnotatedWith(SecondaryPort.class)
                    .allowEmptyShould(true)
                    .because("PayloadSerializer is an outbound serialization port");

    public static final ArchRule infrastructure_mybatis_message_stores_should_be_secondary_adapters =
            classes()
                    .that().resideInAPackage("org.jfoundry.infrastructure..mybatis..")
                    .and().haveSimpleNameEndingWith("MessageStore")
                    .should().beAnnotatedWith(SecondaryAdapter.class)
                    .allowEmptyShould(true)
                    .because("MyBatis message stores implement secondary application ports");

    public static final ArchRule kafka_message_sender_should_be_secondary_adapter =
            classes()
                    .that().haveFullyQualifiedName("org.jfoundry.infrastructure.messaging.kafka.KafkaMessageSender")
                    .should().beAnnotatedWith(SecondaryAdapter.class)
                    .allowEmptyShould(true)
                    .because("KafkaMessageSender adapts the MessageSender secondary port");

    public static final ArchRule jackson_payload_serializer_should_be_secondary_adapter =
            classes()
                    .that().haveFullyQualifiedName("org.jfoundry.infrastructure.messaging.jackson.JacksonPayloadSerializer")
                    .should().beAnnotatedWith(SecondaryAdapter.class)
                    .allowEmptyShould(true)
                    .because("JacksonPayloadSerializer adapts the PayloadSerializer secondary port");
}
