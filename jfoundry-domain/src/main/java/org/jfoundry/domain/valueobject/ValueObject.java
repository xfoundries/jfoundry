package org.jfoundry.domain.valueobject;

/// 领域层值对象标记接口。
/// <p>
/// 业务值对象实现此接口即获得：
/// <ul>
///   <li>jmolecules {@code ValueObject} 类型语义（可被 jmolecules 生态工具识别）</li>
///   <li>框架 ArchUnit 规则保护（强制不可变、强制 equals/hashCode，见 {@code ValueObjectRules}）</li>
/// </ul>
/// <p>
/// 推荐使用 Java 21 {@code record} 作为实现载体——record 天生满足不可变和 equals/hashCode 契约。
/// 若使用 class 实现，必须：
/// <ul>
///   <li>声明为 {@code final}</li>
///   <li>所有字段 {@code final}</li>
///   <li>重写 {@code equals} / {@code hashCode}</li>
/// </ul>
/// <p>
/// 本接口不添加任何方法——它是纯标记接口，仅用于类型擦除后的业务可见性。
public interface ValueObject extends org.jmolecules.ddd.types.ValueObject {
}
