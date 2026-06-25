package org.jfoundry.test.archunit;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.jfoundry.domain.valueobject.ValueObject;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/// 值对象架构规则集合。
/// <p>
/// 强制执行 spec Section 8.1 的约束：值对象必须不可变、必须有 equals/hashCode。
/// <p>
/// 业务侧用法：
/// <pre>
/// &#64;AnalyzeClasses(packages = "com.mysoft.ci")
/// class CiArchitectureTest {
///     &#64;ArchTest
///     ArchRule[] valueObjectRules = JFoundryRules.hexagonal();
/// }
/// </pre>
public final class ValueObjectRules {

    private ValueObjectRules() {
    }

    /// 值对象实现类必须是 record 或 final class。
    /// <p>
    /// record 天生 final、天生不可变、天生有 equals/hashCode，是首选载体；
    /// 若用 class 实现，必须显式 final 以防子类破坏不可变性。
    /// <p>
    /// {@code allowEmptyShould(true)}：本规则由框架分发，业务代码可能在尚未声明任何 ValueObject
    /// 时引用；此时规则应视为「空过」（vacuously pass）而非失败——ArchUnit 默认对空 should 报错
    /// 是为捕获本地规则拼写错误，但库规则需要支持「尚未应用」的合法场景。
    public static final ArchRule value_objects_must_be_final =
            classes()
                    .that().implement(ValueObject.class)
                    .should().beRecords()
                    .orShould().haveModifier(JavaModifier.FINAL)
                    .allowEmptyShould(true)
                    .because("ValueObject must be immutable; records are immutable by default, "
                            + "class implementations must be final to prevent subclassing");

    /// 值对象的所有字段必须 final。
    /// <p>
    /// record 字段天生 final；这条规则主要约束 class 实现的值对象。
    public static final ArchRule value_object_fields_must_be_final =
            classes()
                    .that().implement(ValueObject.class)
                    .should().haveOnlyFinalFields()
                    .allowEmptyShould(true)
                    .because("ValueObject fields must be final to guarantee immutability");

    /// 值对象必须实现 equals 和 hashCode。
    /// <p>
    /// record 天生实现；class 实现必须显式重写。两个相等的值对象必须产生相同的 hashCode。
    public static final ArchRule value_objects_must_implement_equals_and_hashCode =
            classes()
                    .that().implement(ValueObject.class)
                    .should(haveEqualsAndHashCode())
                    .allowEmptyShould(true)
                    .because("ValueObject must implement equals and hashCode for value semantics");

    /// ArchUnit 1.4.2 的 {@code classes().should(...)} 只接受 {@link ArchCondition}，
    /// 不接受 {@code DescribedPredicate}，因此这里用自定义 {@link ArchCondition}
    /// （与 {@code PersistenceRules} 同样的处理方式）。
    /// <p>
    /// 判定标准：
    /// <ul>
    ///   <li>存在名为 {@code equals} 的非-native 方法，且形参列表长度为 1</li>
    ///   <li>存在名为 {@code hashCode} 的非-native 方法，且无形参</li>
    /// </ul>
    /// 排除 {@code native} 是为了避免 JVM 内置的 {@code java.lang.Object#hashCode} 等被误判为「已实现」。
    private static ArchCondition<JavaClass> haveEqualsAndHashCode() {
        return new ArchCondition<JavaClass>("implement equals and hashCode") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                boolean hasEquals = item.getMethods().stream()
                        .anyMatch(m -> "equals".equals(m.getName())
                                && m.getRawParameterTypes().size() == 1
                                && !m.getModifiers().contains(JavaModifier.NATIVE));
                boolean hasHashCode = item.getMethods().stream()
                        .anyMatch(m -> "hashCode".equals(m.getName())
                                && m.getRawParameterTypes().isEmpty()
                                && !m.getModifiers().contains(JavaModifier.NATIVE));
                if (!hasEquals || !hasHashCode) {
                    events.add(SimpleConditionEvent.violated(item,
                            item.getSimpleName() + " does not implement both equals(Object) and hashCode()"));
                }
            }
        };
    }
}
