package org.jfoundry.test.archunit;

import com.tngtech.archunit.lang.ArchRule;
import org.jmolecules.archunit.JMoleculesArchitectureRules;
import org.jmolecules.archunit.JMoleculesDddRules;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/// jfoundry 架构规则聚合入口。
/// <p>
/// 业务侧用法（推荐）：
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
/// }
/// </pre>
/// <p>
/// {@link #hexagonal()}、{@link #onionSimple()} 和 {@link #onionClassical()}
/// 返回 JFoundry 基础守护规则 + 单一主架构风格规则；
/// {@link #jmoleculesDdd()} 返回 jmolecules 官方提供的 DDD 规则。
public final class JFoundryRules {

    private JFoundryRules() {
    }

    /// Hexagonal Architecture / Ports and Adapters 规则。
    /// <p>
    /// 包含 JFoundry 基础守护规则、jMolecules Hexagonal 原生规则，以及 Hexagonal/Onion 互斥规则。
    public static ArchRule[] hexagonal() {
        return concat(base(), new ArchRule[]{
                JMoleculesArchitectureRules.ensureHexagonal(),
                ArchitectureStyleRules.hexagonal_and_onion_must_not_be_mixed
        });
    }

    /// Onion Architecture simplified 规则。
    /// <p>
    /// 包含 JFoundry 基础守护规则、jMolecules Onion Simple 原生规则，以及 Hexagonal/Onion 互斥规则。
    public static ArchRule[] onionSimple() {
        return concat(base(), new ArchRule[]{
                JMoleculesArchitectureRules.ensureOnionSimple(),
                ArchitectureStyleRules.hexagonal_and_onion_must_not_be_mixed
        });
    }

    /// Onion Architecture classical 规则。
    /// <p>
    /// 包含 JFoundry 基础守护规则、jMolecules Onion Classical 原生规则，以及 Hexagonal/Onion 互斥规则。
    public static ArchRule[] onionClassical() {
        return concat(base(), new ArchRule[]{
                JMoleculesArchitectureRules.ensureOnionClassical(),
                ArchitectureStyleRules.hexagonal_and_onion_must_not_be_mixed
        });
    }

    /// Hexagonal 与 Onion 主架构风格互斥规则。
    public static ArchRule noMixedHexagonalAndOnion() {
        return ArchitectureStyleRules.hexagonal_and_onion_must_not_be_mixed;
    }

    /// jmolecules 官方提供的 DDD 规则（精选）。
    /// <p>
    /// 来源：{@code org.jmolecules.integrations:jmolecules-archunit}。
    /// <p>
    /// 选取 jmolecules 的稳定原生规则：
    /// <ul>
    ///   <li>{@link JMoleculesDddRules#aggregateReferencesShouldBeViaIdOrAssociation()} —
    ///       聚合之间只能通过 Id 或关联引用，避免直接对象引用导致的边界穿透</li>
    ///   <li>{@link JMoleculesDddRules#valueObjectsMustNotReferToIdentifiables()} —
    ///       值对象不得引用具备身份的实体或聚合</li>
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
