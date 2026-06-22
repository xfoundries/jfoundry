package org.jfoundry.architecture.layered;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// 应用层标记。标注在 {@code package-info.java} 上声明整个 package 属于应用层。
/// <p>
/// 应用层负责：编排领域对象完成业务用例、管理事务边界、声明领域事件的消费。
/// 应用层不包含业务规则，只编排；业务规则归属领域层。
/// <p>
/// 依赖方向：应用层 → 领域层；禁止依赖适配器层与基础设施层。
@org.jmolecules.architecture.layered.ApplicationLayer
@Target({ElementType.PACKAGE, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApplicationLayer {
}
