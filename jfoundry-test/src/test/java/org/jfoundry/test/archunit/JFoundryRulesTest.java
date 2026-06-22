package org.jfoundry.test.archunit;

import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// P3-3: JFoundryRules must aggregate framework rules + jmolecules native rules.
class JFoundryRulesTest {

    @Test
    void allReturnsAtLeastSevenRules() {
        // 3 ValueObject + 2 Layered + 2 Persistence = 7
        ArchRule[] all = JFoundryRules.all();
        assertThat(all).hasSizeGreaterThanOrEqualTo(7);
    }

    @Test
    void jmoleculesNativeReturnsAtLeastThreeRules() {
        ArchRule[] nativeRules = JFoundryRules.jmoleculesNative();
        assertThat(nativeRules).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    void allRulesAreNonNull() {
        for (ArchRule rule : JFoundryRules.all()) {
            assertThat(rule).as("rule in JFoundryRules.all() must not be null").isNotNull();
        }
        for (ArchRule rule : JFoundryRules.jmoleculesNative()) {
            assertThat(rule).as("rule in JFoundryRules.jmoleculesNative() must not be null").isNotNull();
        }
    }
}
