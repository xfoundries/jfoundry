package org.jfoundry.test.archunit;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaPackage;
import com.tngtech.archunit.lang.ArchRule;
import org.jfoundry.architecture.layered.ApplicationLayer;
import org.jfoundry.architecture.layered.DomainLayer;
import org.jfoundry.architecture.layered.InfrastructureLayer;
import org.jfoundry.architecture.layered.InterfaceLayer;
import org.jmolecules.ddd.types.Repository;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/// 分层架构规则集合。
/// <p>
/// 强制执行 spec Section 8.2 的依赖方向：应用层只能依赖应用层和领域层；
/// 基础设施层不允许被接口层或应用层直接依赖。
/// <p>
/// 业务侧用法：在业务模块的 ArchUnit 测试里通过 {@code JFoundryRules.all()} 一键启用。
public final class LayeredRules {

    private LayeredRules() {
    }

    /// 应用层只能依赖应用层和领域层——不能直接依赖接口层或基础设施层。
    /// <p>
    /// 这是分层架构的核心约束：应用层不直接依赖接口层或基础设施层。
    /// <p>
    /// 规则基于包上的 {@link ApplicationLayer} / {@link DomainLayer} / {@link InterfaceLayer} /
    /// {@link InfrastructureLayer} 标注识别（通过 package-info.java）。
    /// <p>
    /// {@code allowEmptyShould(true)}：本规则由框架分发，业务代码可能在尚未声明任何层标注时引用；
    /// ArchUnit 默认对空 should 报错是为捕获本地规则拼写错误，但库规则需要支持「尚未应用」的合法场景
    /// （与 {@link ValueObjectRules} 同样的处理方式，详见 Task 3.3 review 结论）。
    public static final ArchRule dependencies_must_follow_layer_hierarchy =
            noClasses()
                    .that(resideInAPackageAnnotatedWith(ApplicationLayer.class))
                    .should().dependOnClassesThat(resideInAnyPackageAnnotatedWith(
                            InterfaceLayer.class, InfrastructureLayer.class))
                    .allowEmptyShould(true)
                    .because("Application layer must only depend on Application and Domain layers; "
                            + "adapters and infrastructure are outside the business core");

    /// 只有应用层可以直接调用 Repository；领域层通过端口（Repository 接口）声明需求，
    /// 基础设施层实现 Repository。应用层负责在用例编排中调用 Repository 获取聚合根。
    /// <p>
    /// 本规则约束：Repository 实现必须位于 Infrastructure 层包内；Repository 接口声明
    /// （通常位于 Domain 层）作为接口合法存在；其它情况视为违规（例如把 Repository 实现
    /// 误放到 Application 层或未声明层标注的包内）。
    /// <p>
    /// {@code allowEmptyShould(true)}：业务代码可能在尚未声明任何层标注时引用本库规则，
    /// 空匹配应视为 vacuously pass（与 {@link ValueObjectRules} 同样的处理方式）。
    public static final ArchRule only_application_may_use_repository_directly =
            classes()
                    .that().implement(Repository.class)
                    .and(resideOutsideAPackageAnnotatedWith(InfrastructureLayer.class))
                    .should().beAnnotatedWith(DomainLayer.class)
                    .orShould().beInterfaces()
                    .allowEmptyShould(true)
                    .because("Repository implementations belong in Infrastructure layer; "
                            + "Repository interface declarations belong in Domain layer");

    /// 判定 JavaClass 是否位于标注了指定层 stereotype 的包内（沿父包链向上查找，
    /// 匹配 DDD 中「子包属于父包所在层」的约定）。
    /// <p>
    /// ArchUnit 1.4.2 的 {@code ClassesThat} 没有 {@code resideInAPackageAnnotatedWith} API
    /// （该 API 在更高版本才加入），因此用 {@link DescribedPredicate} 自行实现。
    private static DescribedPredicate<JavaClass> resideInAPackageAnnotatedWith(
            Class<? extends java.lang.annotation.Annotation> layer) {
        String layerName = layer.getSimpleName();
        return DescribedPredicate.describe(
                "reside in a package annotated with @" + layerName,
                (java.util.function.Predicate<JavaClass>) javaClass -> isAnnotatedWithLayer(javaClass, layer));
    }

    /// 判定 JavaClass 是否位于任意一个指定层 stereotype 的包内。
    @SafeVarargs
    private static DescribedPredicate<JavaClass> resideInAnyPackageAnnotatedWith(
            Class<? extends java.lang.annotation.Annotation>... layers) {
        return DescribedPredicate.describe(
                "reside in a package annotated with any of " + layerNames(layers),
                (java.util.function.Predicate<JavaClass>) javaClass -> {
                    for (Class<? extends java.lang.annotation.Annotation> layer : layers) {
                        if (isAnnotatedWithLayer(javaClass, layer)) {
                            return true;
                        }
                    }
                    return false;
                });
    }

    /// 判定 JavaClass 是否位于指定层 stereotype 之外的包内。
    private static DescribedPredicate<JavaClass> resideOutsideAPackageAnnotatedWith(
            Class<? extends java.lang.annotation.Annotation> layer) {
        String layerName = layer.getSimpleName();
        return DescribedPredicate.describe(
                "reside outside a package annotated with @" + layerName,
                (java.util.function.Predicate<JavaClass>) javaClass -> !isAnnotatedWithLayer(javaClass, layer));
    }

    /// 沿 {@link JavaClass} 所在包的父链向上查找是否有标注了指定 stereotype 的包。
    /// <p>
    /// DDD 约定：标注在父包上的层对所有子包生效（例如 {@code com.acme.app.application.controller}
    /// 和 {@code com.acme.app.application} 都属于应用层，只要 application 包上有 {@code @ApplicationLayer}）。
    private static boolean isAnnotatedWithLayer(JavaClass javaClass,
                                                Class<? extends java.lang.annotation.Annotation> layer) {
        JavaPackage pkg = javaClass.getPackage();
        while (pkg != null) {
            if (pkg.isAnnotatedWith(layer)) {
                return true;
            }
            pkg = pkg.getParent().orElse(null);
        }
        return false;
    }

    @SafeVarargs
    private static String layerNames(Class<? extends java.lang.annotation.Annotation>... layers) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < layers.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("@").append(layers[i].getSimpleName());
        }
        return sb.append("]").toString();
    }
}
