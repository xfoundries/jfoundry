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
/// {@link #all()} 返回框架自有的全部规则（Persistence + ValueObject + Layered）；
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

    /// jmolecules 官方提供的 DDD + 架构规则（精选）。
    /// <p>
    /// 来源：{@code org.jmolecules.integrations:jmolecules-archunit}。
    /// <p>
    /// 选取三条最稳定的原生规则：
    /// <ul>
    ///   <li>{@link JMoleculesDddRules#aggregateReferencesShouldBeViaIdOrAssociation()} —
    ///       聚合之间只能通过 Id 或关联引用，避免直接对象引用导致的边界穿透</li>
    ///   <li>{@link JMoleculesDddRules#valueObjectsMustNotReferToIdentifiables()} —
    ///       值对象不得引用具备身份的实体或聚合</li>
    ///   <li>{@link JMoleculesArchitectureRules#ensureLayering()} —
    ///       基于 jmolecules 分层注解的 LayeredArchitecture 约束</li>
    /// </ul>
    public static ArchRule[] jmoleculesNative() {
        return new ArchRule[]{
                JMoleculesDddRules.aggregateReferencesShouldBeViaIdOrAssociation(),
                JMoleculesDddRules.valueObjectsMustNotReferToIdentifiables(),
                JMoleculesArchitectureRules.ensureLayering(),
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
