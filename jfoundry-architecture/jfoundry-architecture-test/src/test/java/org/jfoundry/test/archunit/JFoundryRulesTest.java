package org.jfoundry.test.archunit;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// P3-3: JFoundryRules must expose explicit primary-style entrypoints, not a catch-all all().
class JFoundryRulesTest {

    private static final ClassFileImporter IMPORTER = new ClassFileImporter();

    @Test
    void doesNotExposeAllAggregator() {
        assertThatThrownBy(() -> JFoundryRules.class.getDeclaredMethod("all"))
                .isInstanceOf(NoSuchMethodException.class);
    }

    @Test
    void doesNotExposeArchitectureStyleCombinations() {
        assertThatThrownBy(() -> JFoundryRules.class.getMethod("layered"))
                .isInstanceOf(NoSuchMethodException.class);
        assertThatThrownBy(() -> JFoundryRules.class.getMethod("layeredHexagonal"))
                .isInstanceOf(NoSuchMethodException.class);
        assertThatThrownBy(() -> JFoundryRules.class.getMethod("layeredOnionSimple"))
                .isInstanceOf(NoSuchMethodException.class);
        assertThatThrownBy(() -> JFoundryRules.class.getMethod("layeredOnionClassical"))
                .isInstanceOf(NoSuchMethodException.class);
    }

    @Test
    void doesNotExposeMixedJmoleculesNativeAggregator() {
        assertThatThrownBy(() -> JFoundryRules.class.getMethod("jmoleculesNative"))
                .isInstanceOf(NoSuchMethodException.class);
    }

    @Test
    void jmoleculesDddReturnsDddRulesOnly() {
        ArchRule[] dddRules = JFoundryRules.jmoleculesDdd();
        assertThat(dddRules).hasSize(2);
    }

    @Test
    void exposesArchitectureStylesExplicitly() {
        assertThat(JFoundryRules.hexagonal()).hasSizeGreaterThanOrEqualTo(5);
        assertThat(JFoundryRules.onionSimple()).hasSizeGreaterThanOrEqualTo(5);
        assertThat(JFoundryRules.onionClassical()).hasSizeGreaterThanOrEqualTo(5);
        assertThat(JFoundryRules.noMixedHexagonalAndOnion()).isNotNull();
    }

    @Test
    void explicitArchitectureStylesAreNonNull() {
        for (ArchRule rule : JFoundryRules.hexagonal()) {
            assertThat(rule).as("rule in JFoundryRules.hexagonal() must not be null").isNotNull();
        }
        for (ArchRule rule : JFoundryRules.onionSimple()) {
            assertThat(rule).as("rule in JFoundryRules.onionSimple() must not be null").isNotNull();
        }
        for (ArchRule rule : JFoundryRules.onionClassical()) {
            assertThat(rule).as("rule in JFoundryRules.onionClassical() must not be null").isNotNull();
        }
        for (ArchRule rule : JFoundryRules.jmoleculesDdd()) {
            assertThat(rule).as("rule in JFoundryRules.jmoleculesDdd() must not be null").isNotNull();
        }
    }

    @Test
    void hexagonalRulesRecognizeJfoundryMetaAnnotatedStereotypes() {
        assertThatThrownBy(() -> {
            for (ArchRule rule : JFoundryRules.hexagonal()) {
                rule.check(IMPORTER.importPackages(
                        "org.jfoundry.test.archunit.fixture.hexagonalconventions.invalid.primaryadapter"));
            }
        }).isInstanceOf(AssertionError.class);
    }
}
