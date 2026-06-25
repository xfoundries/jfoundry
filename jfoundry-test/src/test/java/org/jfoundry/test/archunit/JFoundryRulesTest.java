package org.jfoundry.test.archunit;

import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// P3-3: JFoundryRules must expose explicit composition entrypoints, not a catch-all all().
class JFoundryRulesTest {

    @Test
    void doesNotExposeAllAggregator() {
        assertThatThrownBy(() -> JFoundryRules.class.getDeclaredMethod("all"))
                .isInstanceOf(NoSuchMethodException.class);
    }

    @Test
    void doesNotExposeStandaloneArchitectureStyleAggregators() {
        assertThatThrownBy(() -> JFoundryRules.class.getMethod("layered"))
                .isInstanceOf(NoSuchMethodException.class);
        assertThatThrownBy(() -> JFoundryRules.class.getMethod("hexagonal"))
                .isInstanceOf(NoSuchMethodException.class);
        assertThatThrownBy(() -> JFoundryRules.class.getMethod("onionSimple"))
                .isInstanceOf(NoSuchMethodException.class);
        assertThatThrownBy(() -> JFoundryRules.class.getMethod("onionClassical"))
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
    void exposesArchitectureStyleCombinationsExplicitly() {
        assertThat(JFoundryRules.layeredHexagonal()).hasSizeGreaterThanOrEqualTo(20);
        assertThat(JFoundryRules.layeredOnionSimple()).hasSizeGreaterThanOrEqualTo(20);
        assertThat(JFoundryRules.layeredOnionClassical()).hasSizeGreaterThanOrEqualTo(20);
        assertThat(JFoundryRules.noMixedHexagonalAndOnion()).isNotNull();
    }

    @Test
    void explicitArchitectureCombinationsAreNonNull() {
        for (ArchRule rule : JFoundryRules.layeredHexagonal()) {
            assertThat(rule).as("rule in JFoundryRules.layeredHexagonal() must not be null").isNotNull();
        }
        for (ArchRule rule : JFoundryRules.layeredOnionSimple()) {
            assertThat(rule).as("rule in JFoundryRules.layeredOnionSimple() must not be null").isNotNull();
        }
        for (ArchRule rule : JFoundryRules.layeredOnionClassical()) {
            assertThat(rule).as("rule in JFoundryRules.layeredOnionClassical() must not be null").isNotNull();
        }
        for (ArchRule rule : JFoundryRules.jmoleculesDdd()) {
            assertThat(rule).as("rule in JFoundryRules.jmoleculesDdd() must not be null").isNotNull();
        }
    }
}
