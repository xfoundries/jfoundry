package org.jfoundry.test.archunit;

import com.tngtech.archunit.lang.ArchRule;
import org.jmolecules.archunit.JMoleculesArchitectureRules;
import org.jmolecules.archunit.JMoleculesDddRules;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/// Aggregated entrypoint for jfoundry architecture rules.
/// <p>
/// Recommended application-side usage:
/// <pre>
/// import com.tngtech.archunit.junit.AnalyzeClasses;
/// import com.tngtech.archunit.junit.ArchTest;
/// import org.jfoundry.test.archunit.JFoundryRules;
///
/// &#64;AnalyzeClasses(packages = "com.mysoft.ci")
/// class CiArchitectureTest {
///     &#64;ArchTest
///     ArchRule[] hexagonalRules = JFoundryRules.hexagonal();
///
///     &#64;ArchTest
///     ArchRule[] jmoleculesDddRules = JFoundryRules.jmoleculesDdd();
///
///     &#64;ArchTest
///     ArchRule[] cqrsRules = JFoundryRules.cqrs();
/// }
/// </pre>
/// <p>
/// {@link #hexagonal()}, {@link #onionSimple()}, and {@link #onionClassical()} return JFoundry
/// baseline guard rules plus one primary architecture style rule set.
/// {@link #jmoleculesDdd()} returns selected official jMolecules DDD rules.
/// {@link #cqrs()} returns optional CQRS rules;
/// {@link #aggregateRepositoryConventions()} returns optional aggregate repository boundary conventions.
public final class JFoundryRules {

    private JFoundryRules() {
    }

    /// Hexagonal Architecture / Ports and Adapters rules.
    /// <p>
    /// Includes JFoundry baseline guard rules, native jMolecules Hexagonal rules, and the
    /// Hexagonal/Onion mutual-exclusion rule.
    public static ArchRule[] hexagonal() {
        return concat(base(), new ArchRule[]{
                JMoleculesArchitectureRules.ensureHexagonal(),
                ArchitectureStyleRules.hexagonal_and_onion_must_not_be_mixed
        });
    }

    /// Recommended implementation conventions for Hexagonal Architecture / Ports and Adapters.
    /// <p>
    /// This entrypoint adds type-shape, package-name, and adapter-isolation conventions that are
    /// outside the native jMolecules role dependency rules.
    public static ArchRule[] hexagonalConventions() {
        return publicStaticArchRules(HexagonalConventionRules.class).toArray(new ArchRule[0]);
    }

    /// Strict Hexagonal Architecture rules.
    /// <p>
    /// Includes the dependency rules from {@link #hexagonal()} and the JFoundry recommended
    /// implementation conventions from {@link #hexagonalConventions()}.
    public static ArchRule[] hexagonalStrict() {
        return concat(hexagonal(), hexagonalConventions());
    }

    /// Simplified Onion Architecture rules.
    /// <p>
    /// Includes JFoundry baseline guard rules, native jMolecules Onion Simple rules, and the
    /// Hexagonal/Onion mutual-exclusion rule.
    public static ArchRule[] onionSimple() {
        return concat(base(), new ArchRule[]{
                JMoleculesArchitectureRules.ensureOnionSimple(),
                ArchitectureStyleRules.hexagonal_and_onion_must_not_be_mixed
        });
    }

    /// Classical Onion Architecture rules.
    /// <p>
    /// Includes JFoundry baseline guard rules, native jMolecules Onion Classical rules, and the
    /// Hexagonal/Onion mutual-exclusion rule.
    public static ArchRule[] onionClassical() {
        return concat(base(), new ArchRule[]{
                JMoleculesArchitectureRules.ensureOnionClassical(),
                ArchitectureStyleRules.hexagonal_and_onion_must_not_be_mixed
        });
    }

    /// Mutual-exclusion rule for Hexagonal and Onion as primary architecture styles.
    public static ArchRule noMixedHexagonalAndOnion() {
        return ArchitectureStyleRules.hexagonal_and_onion_must_not_be_mixed;
    }

    /// CQRS rules.
    /// <p>
    /// jMolecules currently does not provide an independent CQRS ArchUnit rule set, so JFoundry
    /// provides this lightweight constraint entrypoint. These rules are not included in the primary
    /// architecture style rules by default; applications must opt in explicitly.
    public static ArchRule[] cqrs() {
        return publicStaticArchRules(CqrsRules.class).toArray(new ArchRule[0]);
    }

    /// Aggregate Repository convention rules.
    /// <p>
    /// This rule group is not included in the primary architecture style entrypoints by default.
    /// Applications should opt in explicitly when they are ready to guard aggregate repositories
    /// against generic query conditions, paging APIs, and persistence service or mapper APIs.
    public static ArchRule[] aggregateRepositoryConventions() {
        return publicStaticArchRules(AggregateRepositoryConventionRules.class).toArray(new ArchRule[0]);
    }

    /// Selected official jMolecules DDD rules.
    /// <p>
    /// Source: {@code org.jmolecules.integrations:jmolecules-archunit}.
    /// <p>
    /// Selected stable native jMolecules rules:
    /// <ul>
    ///   <li>{@link JMoleculesDddRules#aggregateReferencesShouldBeViaIdOrAssociation()} —
    ///       aggregates may reference each other only through IDs or associations, avoiding boundary
    ///       leaks caused by direct object references.</li>
    ///   <li>{@link JMoleculesDddRules#valueObjectsMustNotReferToIdentifiables()} —
    ///       value objects must not refer to identifiable entities or aggregates.</li>
    /// </ul>
    public static ArchRule[] jmoleculesDdd() {
        return new ArchRule[]{
                JMoleculesDddRules.aggregateReferencesShouldBeViaIdOrAssociation(),
                JMoleculesDddRules.valueObjectsMustNotReferToIdentifiables()
        };
    }

    private static ArchRule[] base() {
        List<ArchRule> collected = new ArrayList<>();
        collected.addAll(publicStaticArchRules(PersistenceRules.class));
        collected.addAll(publicStaticArchRules(ValueObjectRules.class));
        collected.addAll(publicStaticArchRules(FrameworkModuleRules.class));
        return collected.toArray(new ArchRule[0]);
    }

    private static ArchRule[] concat(ArchRule[]... groups) {
        int length = 0;
        for (ArchRule[] group : groups) {
            length += group.length;
        }
        ArchRule[] merged = new ArchRule[length];
        int offset = 0;
        for (ArchRule[] group : groups) {
            System.arraycopy(group, 0, merged, offset, group.length);
            offset += group.length;
        }
        return merged;
    }

    private static List<ArchRule> publicStaticArchRules(Class<?> rulesClass) {
        return Arrays.stream(rulesClass.getDeclaredFields())
                .filter(f -> (f.getModifiers() & Modifier.STATIC) != 0)
                .filter(f -> ArchRule.class.isAssignableFrom(f.getType()))
                .flatMap(f -> {
                    try {
                        f.setAccessible(true);
                        return Stream.of((ArchRule) f.get(null));
                    } catch (IllegalAccessException e) {
                        throw new IllegalStateException(
                                "Failed to access ArchRule field " + rulesClass.getName() + "#" + f.getName(), e);
                    }
                })
                .toList();
    }
}
