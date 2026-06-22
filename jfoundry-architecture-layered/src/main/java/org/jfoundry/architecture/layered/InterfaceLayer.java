package org.jfoundry.architecture.layered;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// 接口层（适配器层）标记。标注在 {@code package-info.java} 上声明整个 package 属于接口层。
/// <p>
/// 接口层包含：REST 控制器、MVC 控制器、MQTT/消息监听器、定时任务、gRPC 服务实现等
/// 接收外部输入的组件。
/// <p>
/// 依赖方向：接口层 → 应用层；禁止直接调用领域层或基础设施层。
@org.jmolecules.architecture.layered.InterfaceLayer
@Target({ElementType.PACKAGE, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface InterfaceLayer {
}
