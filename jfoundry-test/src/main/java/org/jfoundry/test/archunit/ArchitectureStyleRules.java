package org.jfoundry.test.archunit;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaPackage;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import java.lang.annotation.Annotation;
import java.util.Collection;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/// 架构风格选择规则。
/// <p>
/// Hexagonal 与 Onion 都定义应用核心和外部技术的主依赖模型。业务代码应选择其中一种作为主风格；
/// Layered 只表达责任分层，可以和任一主风格组合使用。
public final class ArchitectureStyleRules {

    private ArchitectureStyleRules() {
    }

    /// 同一个 ArchUnit 分析范围内不应同时出现 Hexagonal 与 Onion 风格标注。
    public static final ArchRule hexagonal_and_onion_must_not_be_mixed =
            classes()
                    .should(new HexagonalAndOnionMustNotBeMixedCondition())
                    .allowEmptyShould(true)
                    .because("Hexagonal and Onion are alternative primary architecture styles; "
                            + "Layered may be combined with either one");

    private static final class HexagonalAndOnionMustNotBeMixedCondition extends ArchCondition<JavaClass> {

        private boolean hasHexagonal;

        private boolean hasOnion;

        private String hexagonalExample;

        private String onionExample;

        HexagonalAndOnionMustNotBeMixedCondition() {
            super("not mix Hexagonal and Onion architecture styles");
        }

        @Override
        public void init(Collection<JavaClass> allClasses) {
            hasHexagonal = false;
            hasOnion = false;
            hexagonalExample = null;
            onionExample = null;
        }

        @Override
        public void check(JavaClass item, ConditionEvents events) {
            if (hasArchitectureStyleAnnotation(item, HEXAGONAL_ANNOTATIONS)) {
                hasHexagonal = true;
                if (hexagonalExample == null) {
                    hexagonalExample = item.getName();
                }
            }
            if (hasArchitectureStyleAnnotation(item, ONION_ANNOTATIONS)) {
                hasOnion = true;
                if (onionExample == null) {
                    onionExample = item.getName();
                }
            }
        }

        @Override
        public void finish(ConditionEvents events) {
            if (hasHexagonal && hasOnion) {
                String message = "Hexagonal and Onion architecture styles must not be mixed in one analyzed scope"
                        + " (hexagonal example: " + hexagonalExample
                        + ", onion example: " + onionExample + ")";
                events.add(SimpleConditionEvent.violated(this, message));
            }
        }
    }

    private static boolean hasArchitectureStyleAnnotation(
            JavaClass javaClass, Class<? extends Annotation>[] annotations) {
        if (isAnnotatedWithAny(javaClass, annotations)) {
            return true;
        }
        JavaPackage pkg = javaClass.getPackage();
        while (pkg != null) {
            if (isAnnotatedWithAny(pkg, annotations)) {
                return true;
            }
            pkg = pkg.getParent().orElse(null);
        }
        return false;
    }

    private static boolean isAnnotatedWithAny(JavaClass javaClass, Class<? extends Annotation>[] annotations) {
        for (Class<? extends Annotation> annotation : annotations) {
            if (javaClass.isAnnotatedWith(annotation.getName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAnnotatedWithAny(JavaPackage javaPackage, Class<? extends Annotation>[] annotations) {
        for (Class<? extends Annotation> annotation : annotations) {
            if (javaPackage.isAnnotatedWith(annotation.getName())) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static final Class<? extends Annotation>[] HEXAGONAL_ANNOTATIONS = new Class[]{
            org.jfoundry.architecture.hexagonal.Application.class,
            org.jfoundry.architecture.hexagonal.Adapter.class,
            org.jfoundry.architecture.hexagonal.Port.class,
            org.jfoundry.architecture.hexagonal.PrimaryAdapter.class,
            org.jfoundry.architecture.hexagonal.PrimaryPort.class,
            org.jfoundry.architecture.hexagonal.SecondaryAdapter.class,
            org.jfoundry.architecture.hexagonal.SecondaryPort.class,
            org.jmolecules.architecture.hexagonal.Application.class,
            org.jmolecules.architecture.hexagonal.Adapter.class,
            org.jmolecules.architecture.hexagonal.Port.class,
            org.jmolecules.architecture.hexagonal.PrimaryAdapter.class,
            org.jmolecules.architecture.hexagonal.PrimaryPort.class,
            org.jmolecules.architecture.hexagonal.SecondaryAdapter.class,
            org.jmolecules.architecture.hexagonal.SecondaryPort.class
    };

    @SuppressWarnings("unchecked")
    private static final Class<? extends Annotation>[] ONION_ANNOTATIONS = new Class[]{
            org.jfoundry.architecture.onion.simplified.DomainRing.class,
            org.jfoundry.architecture.onion.simplified.ApplicationRing.class,
            org.jfoundry.architecture.onion.simplified.InfrastructureRing.class,
            org.jfoundry.architecture.onion.classical.DomainModelRing.class,
            org.jfoundry.architecture.onion.classical.DomainServiceRing.class,
            org.jfoundry.architecture.onion.classical.ApplicationServiceRing.class,
            org.jfoundry.architecture.onion.classical.InfrastructureRing.class,
            org.jmolecules.architecture.onion.simplified.DomainRing.class,
            org.jmolecules.architecture.onion.simplified.ApplicationRing.class,
            org.jmolecules.architecture.onion.simplified.InfrastructureRing.class,
            org.jmolecules.architecture.onion.classical.DomainModelRing.class,
            org.jmolecules.architecture.onion.classical.DomainServiceRing.class,
            org.jmolecules.architecture.onion.classical.ApplicationServiceRing.class,
            org.jmolecules.architecture.onion.classical.InfrastructureRing.class
    };
}
