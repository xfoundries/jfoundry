package org.jfoundry.test.archunit;

import com.tngtech.archunit.lang.ArchRule;
import org.jmolecules.archunit.JMoleculesArchitectureRules;
import org.jmolecules.archunit.JMoleculesDddRules;

import java.lang.reflect.Field;
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
///     ArchRule[] jfoundryRules = JFoundryRules.all();
///
///     &#64;ArchTest
///     ArchRule[] jmoleculesNativeRules = JFoundryRules.jmoleculesNative();
/// }
/// </pre>
/// <p>
/// {@link #all()} 返回框架自有的默认规则（Persistence + ValueObject + Layered）；
/// {@link #jmoleculesNative()} 返回 jmolecules 官方提供的 DDD + 架构规则。
public final class JFoundryRules {

    private JFoundryRules() {
    }

    /// 框架自有的所有 ArchUnit 规则聚合。
    /// <p>
    /// 包含：
    /// <ul>
    ///   <li>{@link PersistenceRules} — 持久化层零 @Transactional、autoconfig 零 @Component</li>
    ///   <li>{@link ValueObjectRules} — 值对象必须不可变、必须有 equals/hashCode</li>
    ///   <li>{@link LayeredRules} — 分层依赖方向约束</li>
    /// </ul>
    public static ArchRule[] all() {
        List<ArchRule> collected = new ArrayList<>();
        collected.addAll(publicStaticArchRules(PersistenceRules.class));
        collected.addAll(publicStaticArchRules(ValueObjectRules.class));
        collected.addAll(publicStaticArchRules(LayeredRules.class));
        return collected.toArray(new ArchRule[0]);
    }

    /// JFoundry 自有分层架构规则。
    public static ArchRule[] layered() {
        return publicStaticArchRules(LayeredRules.class).toArray(new ArchRule[0]);
    }

    /// Hexagonal Architecture 规则。
    /// <p>
    /// 包含 jMolecules 原生 Hexagonal 校验，以及 JFoundry 对 Hexagonal/Onion 互斥选择的保护。
    public static ArchRule[] hexagonal() {
        return new ArchRule[]{
                JMoleculesArchitectureRules.ensureHexagonal(),
                ArchitectureStyleRules.hexagonal_and_onion_must_not_be_mixed
        };
    }

    /// 简化 Onion Architecture 规则。
    /// <p>
    /// 包含 jMolecules 原生 Onion Simple 校验，以及 JFoundry 对 Hexagonal/Onion 互斥选择的保护。
    public static ArchRule[] onionSimple() {
        return new ArchRule[]{
                JMoleculesArchitectureRules.ensureOnionSimple(),
                ArchitectureStyleRules.hexagonal_and_onion_must_not_be_mixed
        };
    }

    /// 经典 Onion Architecture 规则。
    /// <p>
    /// 包含 jMolecules 原生 Onion Classical 校验，以及 JFoundry 对 Hexagonal/Onion 互斥选择的保护。
    public static ArchRule[] onionClassical() {
        return new ArchRule[]{
                JMoleculesArchitectureRules.ensureOnionClassical(),
                ArchitectureStyleRules.hexagonal_and_onion_must_not_be_mixed
        };
    }

    /// Hexagonal 与 Onion 主架构风格互斥规则。
    public static ArchRule noMixedHexagonalAndOnion() {
        return ArchitectureStyleRules.hexagonal_and_onion_must_not_be_mixed;
    }

    /// jmolecules 官方提供的 DDD + 架构规则（精选）。
    /// <p>
    /// 来源：{@code org.jmolecules.integrations:jmolecules-archunit}。
    /// <p>
    /// 选取 jmolecules 的稳定原生规则：
    /// <ul>
    ///   <li>{@link JMoleculesDddRules#aggregateReferencesShouldBeViaIdOrAssociation()} —
    ///       聚合之间只能通过 Id 或关联引用，避免直接对象引用导致的边界穿透</li>
    ///   <li>{@link JMoleculesDddRules#valueObjectsMustNotReferToIdentifiables()} —
    ///       值对象不得引用具备身份的实体或聚合</li>
    ///   <li>{@link JMoleculesArchitectureRules#ensureLayering()} —
    ///       基于 jmolecules 分层注解的 LayeredArchitecture 约束</li>
    ///   <li>{@link JMoleculesArchitectureRules#ensureHexagonal()} —
    ///       基于 jmolecules Hexagonal 注解的 Ports and Adapters 约束</li>
    ///   <li>{@link JMoleculesArchitectureRules#ensureOnionSimple()} /
    ///       {@link JMoleculesArchitectureRules#ensureOnionClassical()} —
    ///       基于 jmolecules Onion 注解的环形依赖约束</li>
    /// </ul>
    public static ArchRule[] jmoleculesNative() {
        return new ArchRule[]{
                JMoleculesDddRules.aggregateReferencesShouldBeViaIdOrAssociation(),
                JMoleculesDddRules.valueObjectsMustNotReferToIdentifiables(),
                JMoleculesArchitectureRules.ensureLayering(),
                JMoleculesArchitectureRules.ensureHexagonal(),
                JMoleculesArchitectureRules.ensureOnionSimple(),
                JMoleculesArchitectureRules.ensureOnionClassical(),
        };
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
