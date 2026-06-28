package org.jfoundry.test.archunit;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaPackage;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/// Hexagonal Architecture / Ports and Adapters convention rules.
/// <p>
/// These rules complement jmolecules' role dependency rules with package and type-shape
/// conventions used by JFoundry projects.
public final class HexagonalConventionRules {

    private static final String[] INBOUND_PORT_PACKAGES = {
            "..port.in..",
            "..ports.in..",
            "..port.inbound..",
            "..ports.inbound..",
            "..usecase..",
            "..usecases.."
    };

    private static final String[] OUTBOUND_PORT_PACKAGES = {
            "..port.out..",
            "..ports.out..",
            "..port.outbound..",
            "..ports.outbound.."
    };

    private static final String[] APPLICATION_FORBIDDEN_PACKAGES = {
            "..adapter..",
            "..adapters..",
            "..controller..",
            "..controllers..",
            "..infrastructure..",
            "..infra..",
            "..mybatis..",
            "..persistence.."
    };

    private static final String[] PERSISTENCE_DETAIL_PACKAGES = {
            "..mybatis..",
            "..mapper..",
            "..data..",
            "..persistence..",
            "..infrastructure.persistence.."
    };

    private static final String JFOUNDRY_APPLICATION =
            "org.jfoundry.architecture.hexagonal.Application";

    private static final String JFOUNDRY_PRIMARY_ADAPTER =
            "org.jfoundry.architecture.hexagonal.PrimaryAdapter";

    private static final String JFOUNDRY_PRIMARY_PORT =
            "org.jfoundry.architecture.hexagonal.PrimaryPort";

    private static final String JFOUNDRY_SECONDARY_ADAPTER =
            "org.jfoundry.architecture.hexagonal.SecondaryAdapter";

    private static final String JFOUNDRY_SECONDARY_PORT =
            "org.jfoundry.architecture.hexagonal.SecondaryPort";

    private static final String JMOLECULES_APPLICATION =
            "org.jmolecules.architecture.hexagonal.Application";

    private static final String JMOLECULES_PRIMARY_ADAPTER =
            "org.jmolecules.architecture.hexagonal.PrimaryAdapter";

    private static final String JMOLECULES_PRIMARY_PORT =
            "org.jmolecules.architecture.hexagonal.PrimaryPort";

    private static final String JMOLECULES_SECONDARY_ADAPTER =
            "org.jmolecules.architecture.hexagonal.SecondaryAdapter";

    private static final String JMOLECULES_SECONDARY_PORT =
            "org.jmolecules.architecture.hexagonal.SecondaryPort";

    private HexagonalConventionRules() {
    }

    /// Primary ports are inbound API contracts and should be interfaces.
    public static final ArchRule primary_ports_must_be_interfaces =
            classes()
                    .that(areHexagonalPrimaryPorts())
                    .should().beInterfaces()
                    .allowEmptyShould(true)
                    .because("primary ports expose inbound application contracts and should be interfaces");

    /// Primary ports should live in inbound port packages.
    public static final ArchRule primary_ports_must_reside_in_inbound_port_packages =
            classes()
                    .that(areHexagonalPrimaryPorts())
                    .should().resideInAnyPackage(INBOUND_PORT_PACKAGES)
                    .allowEmptyShould(true)
                    .because("primary ports should be easy to locate under port.in / ports.in / usecase packages");

    /// Secondary ports are outbound API contracts and should be interfaces.
    public static final ArchRule secondary_ports_must_be_interfaces =
            classes()
                    .that(areHexagonalSecondaryPorts())
                    .should().beInterfaces()
                    .allowEmptyShould(true)
                    .because("secondary ports describe outbound needs and should be interfaces");

    /// Secondary ports should live in outbound port packages.
    public static final ArchRule secondary_ports_must_reside_in_outbound_port_packages =
            classes()
                    .that(areHexagonalSecondaryPorts())
                    .should().resideInAnyPackage(OUTBOUND_PORT_PACKAGES)
                    .allowEmptyShould(true)
                    .because("secondary ports should be easy to locate under port.out / ports.out packages");

    /// Application core code should not be placed in adapter or infrastructure packages.
    public static final ArchRule applications_must_not_reside_in_adapter_packages =
            noClasses()
                    .that(areHexagonalApplications())
                    .should().resideInAnyPackage(APPLICATION_FORBIDDEN_PACKAGES)
                    .allowEmptyShould(true)
                    .because("application core code should not be packaged as delivery, infrastructure, or persistence adapters");

    /// Primary adapters should drive the application through primary ports, not through the outbound side.
    public static final ArchRule primary_adapters_must_not_depend_on_secondary_ports_or_adapters =
            noClasses()
                    .that(areHexagonalPrimaryAdapters())
                    .should(dependOnSecondaryPortsOrAdapters())
                    .allowEmptyShould(true)
                    .because("primary adapters should call primary ports/use cases and must not bypass the application core");

    /// Secondary adapters should implement at least one secondary port.
    public static final ArchRule secondary_adapters_should_implement_secondary_ports =
            classes()
                    .that(areHexagonalSecondaryAdapters())
                    .should(implementSecondaryPort())
                    .allowEmptyShould(true)
                    .because("secondary adapters should fulfill outbound port contracts");

    /// Application and domain code must not depend on persistence implementation details.
    public static final ArchRule application_and_domain_must_not_depend_on_persistence_details =
            noClasses()
                    .that().resideInAnyPackage("..application..", "..app..", "..domain..", "..service..")
                    .or().areAnnotatedWith(JMOLECULES_APPLICATION)
                    .or().areMetaAnnotatedWith(JMOLECULES_APPLICATION)
                    .or().areAnnotatedWith(JFOUNDRY_APPLICATION)
                    .should().dependOnClassesThat().resideInAnyPackage(PERSISTENCE_DETAIL_PACKAGES)
                    .allowEmptyShould(true)
                    .because("application and domain code should depend on ports, not MyBatis, mapper, data, or persistence details");

    private static com.tngtech.archunit.base.DescribedPredicate<JavaClass> areHexagonalApplications() {
        return new com.tngtech.archunit.base.DescribedPredicate<>("are hexagonal applications") {
            @Override
            public boolean test(JavaClass input) {
                return hasAnnotationInClassOrPackage(input, JMOLECULES_APPLICATION, JFOUNDRY_APPLICATION);
            }
        };
    }

    private static com.tngtech.archunit.base.DescribedPredicate<JavaClass> areHexagonalPrimaryAdapters() {
        return new com.tngtech.archunit.base.DescribedPredicate<>("are hexagonal primary adapters") {
            @Override
            public boolean test(JavaClass input) {
                return hasAnnotationInClassOrPackage(input, JMOLECULES_PRIMARY_ADAPTER, JFOUNDRY_PRIMARY_ADAPTER);
            }
        };
    }

    private static com.tngtech.archunit.base.DescribedPredicate<JavaClass> areHexagonalPrimaryPorts() {
        return new com.tngtech.archunit.base.DescribedPredicate<>("are hexagonal primary ports") {
            @Override
            public boolean test(JavaClass input) {
                return hasAnnotationInClassOrPackage(input, JMOLECULES_PRIMARY_PORT, JFOUNDRY_PRIMARY_PORT);
            }
        };
    }

    private static com.tngtech.archunit.base.DescribedPredicate<JavaClass> areHexagonalSecondaryAdapters() {
        return new com.tngtech.archunit.base.DescribedPredicate<>("are hexagonal secondary adapters") {
            @Override
            public boolean test(JavaClass input) {
                return hasAnnotationInClassOrPackage(input, JMOLECULES_SECONDARY_ADAPTER, JFOUNDRY_SECONDARY_ADAPTER);
            }
        };
    }

    private static com.tngtech.archunit.base.DescribedPredicate<JavaClass> areHexagonalSecondaryPorts() {
        return new com.tngtech.archunit.base.DescribedPredicate<>("are hexagonal secondary ports") {
            @Override
            public boolean test(JavaClass input) {
                return hasAnnotationInClassOrPackage(input, JMOLECULES_SECONDARY_PORT, JFOUNDRY_SECONDARY_PORT);
            }
        };
    }

    private static ArchCondition<JavaClass> dependOnSecondaryPortsOrAdapters() {
        return new ArchCondition<>("depend on secondary ports or secondary adapters") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                for (Dependency dependency : item.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass();
                    if (hasAnnotationInClassOrPackage(target, JMOLECULES_SECONDARY_PORT, JFOUNDRY_SECONDARY_PORT)
                            || hasAnnotationInClassOrPackage(target, JMOLECULES_SECONDARY_ADAPTER,
                            JFOUNDRY_SECONDARY_ADAPTER)) {
                        events.add(SimpleConditionEvent.satisfied(item, dependency.getDescription()));
                    }
                }
            }
        };
    }

    private static ArchCondition<JavaClass> implementSecondaryPort() {
        return new ArchCondition<>("implement a secondary port") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                for (JavaClass rawInterface : item.getAllRawInterfaces()) {
                    if (hasAnnotationInClassOrPackage(rawInterface, JMOLECULES_SECONDARY_PORT, JFOUNDRY_SECONDARY_PORT)) {
                        return;
                    }
                }
                events.add(SimpleConditionEvent.violated(item,
                        item.getName() + " does not implement a @SecondaryPort interface"));
            }
        };
    }

    private static boolean hasAnnotationInClassOrPackage(JavaClass javaClass, String... annotations) {
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

    private static boolean isAnnotatedWithAny(JavaClass javaClass, String... annotations) {
        for (String annotation : annotations) {
            if (javaClass.isAnnotatedWith(annotation) || javaClass.isMetaAnnotatedWith(annotation)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAnnotatedWithAny(JavaPackage javaPackage, String... annotations) {
        for (String annotation : annotations) {
            if (javaPackage.isAnnotatedWith(annotation)) {
                return true;
            }
        }
        return false;
    }
}
