package org.jfoundry.test.archunit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// P3-2 guard: LayeredRules must be declared and pass against jfoundry's own source.
/// jfoundry's own modules don't use @ApplicationLayer etc. (it's a framework, not business
/// code), so this test only sanity-checks that the rules are valid ArchRule instances.
class LayeredRulesTest {

    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_JARS)
            .importPackages("org.jfoundry");

    @Test
    void layeredRulesAreDeclared() {
        assertThat(LayeredRules.dependencies_must_follow_layer_hierarchy).isNotNull();
        assertThat(LayeredRules.only_application_may_use_repository_directly).isNotNull();
    }

    @Test
    void rulesAreValidAgainstEmptyPackage() {
        // The rules should be no-op on a package with no layer annotations.
        // This catches malformed predicates that would throw at evaluation time.
        LayeredRules.dependencies_must_follow_layer_hierarchy.check(classes);
        LayeredRules.only_application_may_use_repository_directly.check(classes);
    }
}
