package org.jfoundry.test.archunit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// 架构风格规则：Hexagonal 与 Onion 是互斥的主架构风格，Layered 可与任一主风格组合。
class ArchitectureStyleRulesTest {

    private final ClassFileImporter importer = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_JARS);

    @Test
    void ruleIsDeclared() {
        assertThat(ArchitectureStyleRules.hexagonal_and_onion_must_not_be_mixed).isNotNull();
    }

    @Test
    void allowsHexagonalWithoutOnion() {
        JavaClasses classes = importer.importPackages("org.jfoundry.test.archunit.fixture.hexagonal");

        ArchitectureStyleRules.hexagonal_and_onion_must_not_be_mixed.check(classes);
    }

    @Test
    void allowsOnionWithoutHexagonal() {
        JavaClasses classes = importer.importPackages("org.jfoundry.test.archunit.fixture.onion");

        ArchitectureStyleRules.hexagonal_and_onion_must_not_be_mixed.check(classes);
    }

    @Test
    void allowsLayeredCombinedWithHexagonal() {
        JavaClasses classes = importer.importPackages("org.jfoundry.test.archunit.fixture.layeredhexagonal");

        ArchitectureStyleRules.hexagonal_and_onion_must_not_be_mixed.check(classes);
    }

    @Test
    void rejectsMixingHexagonalAndOnionInOneAnalyzedScope() {
        JavaClasses classes = importer.importPackages("org.jfoundry.test.archunit.fixture.mixedstyles");
        ArchRule rule = ArchitectureStyleRules.hexagonal_and_onion_must_not_be_mixed;

        assertThatThrownBy(() -> rule.check(classes))
                .hasMessageContaining("Hexagonal and Onion architecture styles must not be mixed");
    }
}
