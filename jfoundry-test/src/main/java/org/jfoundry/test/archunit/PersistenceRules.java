package org.jfoundry.test.archunit;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/// 持久化层架构规则集合。
/// <p>
/// 强制执行 spec Section 6.3 的约束：事务边界属于应用层，持久化层零 {@code @Transactional}。
public final class PersistenceRules {

    private PersistenceRules() {
    }

    /// 持久化实现包下零 {@code @Transactional}（类级别和方法级别都禁）。
    /// <p>
    /// P1-3 修复的防护网：防止 javadoc 修对后，未来有人误把 @Transactional 加到 Repository 实现上。
    /// <p>
    /// ArchUnit 1.4.2 没有 {@code haveMethodsAnnotatedWith} API，因此用自定义 {@link ArchCondition}
    /// 同时检查类级别和方法级别的 {@code @Transactional}（含元注解）。
    public static final ArchRule persistence_repository_must_not_use_transactional =
            noClasses()
                    .that().resideInAPackage("..infrastructure.persistence..")
                    .should(haveTransactionalAtClassOrMethodLevel())
                    .because("事务边界属于应用层；持久化层 @Transactional 是 P1-3 修复的契约漂移信号");

    /// autoconfig 模块禁止使用 {@code @Component}（P1-1 防护网）。
    /// <p>
    /// autoconfig 类应该用 {@code @AutoConfiguration} + {@code @Bean}，不允许 {@code @ComponentScan}。
    public static final ArchRule autoconfig_must_not_use_component =
            noClasses()
                    .that().resideInAPackage("..autoconfigure..")
                    .should(beAnnotatedWithOrMetaAnnotatedWith(Component.class))
                    .because("autoconfig 模块应使用 @AutoConfiguration + @Bean，禁止 @Component/@ComponentScan (P1-1)");

    private static ArchCondition<JavaClass> haveTransactionalAtClassOrMethodLevel() {
        return new ArchCondition<JavaClass>("be annotated with @Transactional at class or method level") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                if (item.isAnnotatedWith(Transactional.class) || item.isMetaAnnotatedWith(Transactional.class)) {
                    events.add(SimpleConditionEvent.violated(item,
                            item.getSimpleName() + " is annotated with @Transactional at class level"));
                    return;
                }
                for (JavaMethod method : item.getMethods()) {
                    if (method.isAnnotatedWith(Transactional.class) || method.isMetaAnnotatedWith(Transactional.class)) {
                        events.add(SimpleConditionEvent.violated(item,
                                item.getSimpleName() + "#" + method.getName()
                                        + " is annotated with @Transactional at method level"));
                    }
                }
            }
        };
    }

    private static ArchCondition<JavaClass> beAnnotatedWithOrMetaAnnotatedWith(Class<? extends java.lang.annotation.Annotation> annotationType) {
        String desc = "be annotated with @" + annotationType.getSimpleName() + " (directly or meta)";
        return new ArchCondition<JavaClass>(desc) {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                if (item.isAnnotatedWith(annotationType) || item.isMetaAnnotatedWith(annotationType)) {
                    events.add(SimpleConditionEvent.violated(item,
                            item.getSimpleName() + " is annotated with @" + annotationType.getSimpleName()));
                }
            }
        };
    }
}
