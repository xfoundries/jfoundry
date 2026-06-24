package org.jfoundry.architecture.layered;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// 基础设施层标记。标注在 {@code package-info.java} 上声明整个 package 属于基础设施层。
/// <p>
/// 基础设施层包含：仓储实现、外部服务客户端适配器、消息中间件适配、技术基础设施。
/// <p>
/// 依赖方向：基础设施层 → 领域层（实现领域层声明的端口）；禁止被接口层或应用层直接依赖。
@org.jmolecules.architecture.layered.InfrastructureLayer
@Target({ElementType.PACKAGE, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface InfrastructureLayer {
}
