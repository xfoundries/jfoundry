package org.jfoundry.test.archunit;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HexagonalConventionRulesTest {

    private static final ClassFileImporter IMPORTER = new ClassFileImporter();

    @Test
    void exposesHexagonalConventionRules() {
        ArchRule[] rules = JFoundryRules.hexagonalConventions();
        assertThat(rules).hasSizeGreaterThanOrEqualTo(6);
        for (ArchRule rule : rules) {
            assertThat(rule).as("rule in JFoundryRules.hexagonalConventions() must not be null").isNotNull();
        }
    }

    @Test
    void exposesStrictHexagonalRules() {
        ArchRule[] rules = JFoundryRules.hexagonalStrict();
        assertThat(rules.length).isGreaterThan(JFoundryRules.hexagonal().length);
    }

    @Test
    void primaryPortsMustBeInterfacesInInboundPortPackages() {
        assertThatThrownBy(() -> HexagonalConventionRules.primary_ports_must_be_interfaces
                .check(IMPORTER.importPackages("org.jfoundry.test.archunit.fixture.hexagonalconventions.invalid.primaryport")))
                .isInstanceOf(AssertionError.class);
        assertThatThrownBy(() -> HexagonalConventionRules.primary_ports_must_reside_in_inbound_port_packages
                .check(IMPORTER.importPackages("org.jfoundry.test.archunit.fixture.hexagonalconventions.invalid.primaryport")))
                .isInstanceOf(AssertionError.class);

        HexagonalConventionRules.primary_ports_must_be_interfaces
                .check(IMPORTER.importPackages("org.jfoundry.test.archunit.fixture.hexagonalconventions.valid"));
        HexagonalConventionRules.primary_ports_must_reside_in_inbound_port_packages
                .check(IMPORTER.importPackages("org.jfoundry.test.archunit.fixture.hexagonalconventions.valid"));
    }

    @Test
    void secondaryPortsMustBeInterfacesInOutboundPortPackages() {
        assertThatThrownBy(() -> HexagonalConventionRules.secondary_ports_must_be_interfaces
                .check(IMPORTER.importPackages("org.jfoundry.test.archunit.fixture.hexagonalconventions.invalid.secondaryport")))
                .isInstanceOf(AssertionError.class);
        assertThatThrownBy(() -> HexagonalConventionRules.secondary_ports_must_reside_in_outbound_port_packages
                .check(IMPORTER.importPackages("org.jfoundry.test.archunit.fixture.hexagonalconventions.invalid.secondaryport")))
                .isInstanceOf(AssertionError.class);

        HexagonalConventionRules.secondary_ports_must_be_interfaces
                .check(IMPORTER.importPackages("org.jfoundry.test.archunit.fixture.hexagonalconventions.valid"));
        HexagonalConventionRules.secondary_ports_must_reside_in_outbound_port_packages
                .check(IMPORTER.importPackages("org.jfoundry.test.archunit.fixture.hexagonalconventions.valid"));
    }

    @Test
    void applicationsMustStayOutOfAdapterPackages() {
        assertThatThrownBy(() -> HexagonalConventionRules.applications_must_not_reside_in_adapter_packages
                .check(IMPORTER.importPackages("org.jfoundry.test.archunit.fixture.hexagonalconventions.invalid.application")))
                .isInstanceOf(AssertionError.class);

        HexagonalConventionRules.applications_must_not_reside_in_adapter_packages
                .check(IMPORTER.importPackages("org.jfoundry.test.archunit.fixture.hexagonalconventions.valid"));
    }

    @Test
    void primaryAdaptersMustNotDependOnSecondarySide() {
        assertThatThrownBy(() -> HexagonalConventionRules.primary_adapters_must_not_depend_on_secondary_ports_or_adapters
                .check(IMPORTER.importPackages("org.jfoundry.test.archunit.fixture.hexagonalconventions.invalid.primaryadapter")))
                .isInstanceOf(AssertionError.class);

        HexagonalConventionRules.primary_adapters_must_not_depend_on_secondary_ports_or_adapters
                .check(IMPORTER.importPackages("org.jfoundry.test.archunit.fixture.hexagonalconventions.valid"));
    }

    @Test
    void secondaryAdaptersShouldImplementSecondaryPorts() {
        assertThatThrownBy(() -> HexagonalConventionRules.secondary_adapters_should_implement_secondary_ports
                .check(IMPORTER.importPackages("org.jfoundry.test.archunit.fixture.hexagonalconventions.invalid.secondaryadapter")))
                .isInstanceOf(AssertionError.class);

        HexagonalConventionRules.secondary_adapters_should_implement_secondary_ports
                .check(IMPORTER.importPackages("org.jfoundry.test.archunit.fixture.hexagonalconventions.valid"));
    }

    @Test
    void applicationAndDomainMustNotDependOnPersistenceDetails() {
        assertThatThrownBy(() -> HexagonalConventionRules.application_and_domain_must_not_depend_on_persistence_details
                .check(IMPORTER.importPackages("org.jfoundry.test.archunit.fixture.hexagonalconventions.invalid.persistenceleak")))
                .isInstanceOf(AssertionError.class);

        HexagonalConventionRules.application_and_domain_must_not_depend_on_persistence_details
                .check(IMPORTER.importPackages("org.jfoundry.test.archunit.fixture.hexagonalconventions.valid"));
    }
}
