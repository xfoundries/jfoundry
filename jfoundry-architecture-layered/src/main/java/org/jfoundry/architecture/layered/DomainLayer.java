package org.jfoundry.architecture.layered;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// 领域层标记。标注在 {@code package-info.java} 上声明整个 package 属于领域层。
/// <p>
/// 领域层包含：聚合根、实体、值对象、领域服务、领域事件、仓储接口（端口）。
/// 领域层是业务内核——零框架依赖、零基础设施依赖。
@org.jmolecules.architecture.layered.DomainLayer
@Target({ElementType.PACKAGE, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DomainLayer {
}
