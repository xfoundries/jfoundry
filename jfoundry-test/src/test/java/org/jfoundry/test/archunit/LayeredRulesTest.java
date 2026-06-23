package org.jfoundry.test.archunit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// P3-2 guard: LayeredRules 必须被声明且能在空 fixture 上 vacuously 通过。
/// <p>
/// 注意：不能用 {@code importPackages("org.jfoundry")} 扫整个框架 —— jfoundry 自身的
/// {@code AbstractPersistenceRepository} 等基础设施类本身就在 {@code Repository} 实现
/// 体系里，规则会命中它们。框架本身不是"业务代码"，规则的目标是约束业务侧的分层，
/// 所以本测试只在一个真正空、无任何层标注的 fixture 包上验证规则不会抛错。
class LayeredRulesTest {

    /// 任意保证不存在的 fixture 包路径：不存在任何类、没有 package-info、
    /// 也没有层标注。{@link ClassFileImporter#importPackages} 找不到任何类时
    /// 返回空 {@link JavaClasses}，{@code allowEmptyShould(true)} 让规则 vacuously pass。
    private static final String EMPTY_FIXTURE_PACKAGE = "org.jfoundry.test.archunit.fixture.empty";

    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_JARS)
            .importPackages(EMPTY_FIXTURE_PACKAGE);

    @Test
    void layeredRulesAreDeclared() {
        assertThat(LayeredRules.dependencies_must_follow_layer_hierarchy).isNotNull();
        assertThat(LayeredRules.only_application_may_use_repository_directly).isNotNull();
    }

    @Test
    void rulesAreValidAgainstEmptyPackage() {
        // 规则在空 fixture 上应该 vacuously pass。本测试的目的是捕获谓词拼写错误
        // 导致的运行时异常（而不是验证框架代码符合规则）。
        LayeredRules.dependencies_must_follow_layer_hierarchy.check(classes);
        LayeredRules.only_application_may_use_repository_directly.check(classes);
    }
}
