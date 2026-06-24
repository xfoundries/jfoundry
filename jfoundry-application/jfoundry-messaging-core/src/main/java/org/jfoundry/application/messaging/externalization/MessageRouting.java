package org.jfoundry.application.messaging.externalization;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// 消息路由元数据。优先级高于 {@link org.jmolecules.event.annotation.Externalized} 的 value()。
/// <p>
/// 语义约定：本注解只声明 routing（topic/key），是否外部化仍由 {@code @Externalized} 决定。
/// 若类上仅有 {@code @MessageRouting} 而无 {@code @Externalized}，resolver 会 WARN 但不报错。
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MessageRouting {

    /// 外部化目标 topic。
    String topic();

    /// routing key 的 SpEL 表达式，root object 为事件实例本身。空字符串表示无 routing key。
    String key() default "";
}
