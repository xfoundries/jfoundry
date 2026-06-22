package org.jfoundry.domain.repository;

import org.jmolecules.ddd.types.AggregateRoot;
import org.jmolecules.ddd.types.Identifier;
import org.jmolecules.ddd.types.Repository;

import java.util.Collection;

/// 聚合根仓储接口。
/// <p>
/// 仓储模拟聚合根集合(Eric Evans《DDD》"conceptual set"),负责聚合生命周期持久化:
/// - findById: 按聚合标识加载单个聚合根
/// - add: 加入集合(新建,对应 SQL insert)
/// - modify: 修改集合中已存在元素(对应 SQL update)
/// - addAll / modifyAll: 批量版本
/// - remove: 从集合移除(对应 SQL delete)
/// <p>
/// 集合语义说明:
/// - 业务方法名(create / update / cancel 等)和调用上下文(new 出来 vs find 来的)天然决定是新建还是修改,业务侧显式选 add 或 modify,Repository 不替业务做判断
/// - MyBatis 无 persistence context(Unit of Work),无法像 JPA 那样自动脏检查,因此 modify 必须显式调用
/// <p>
/// 设计约束:
/// - 仓储只针对聚合根(AggregateRoot)
/// - 一个事务默认只修改一个聚合根
/// - 读模型、统计、分页和维护清理应通过业务具名边界表达,不通过 Repository 暴露通用条件能力
///
/// @param <T>  聚合根类型
/// @param <ID> 标识符类型
public interface AggregateRepository<T extends AggregateRoot<T, ID>, ID extends Identifier>
        extends Repository<T, ID> {

    /// 根据标识符查找聚合根。
    ///
    /// @param id 标识符
    /// @return 聚合根,如果不存在返回 null
    T findById(ID id);

    /// 加入聚合集合(新建)。
    /// <p>
    /// 直接走 insert 路径。主键冲突由底层数据库抛出(如 DuplicateKeyException),不在 Repository 层做"已存在"预检——保持零额外开销。
    /// 成功后移交聚合根中已经记录的领域事件,并立即清空。
    ///
    /// @param entity 聚合根
    void add(T entity);

    /// 修改聚合集合中已存在的元素。
    /// <p>
    /// 直接走 update 路径。受影响行数为 0 时(即聚合不存在或乐观锁版本冲突)抛出 {@link IllegalStateException},防御"修改了不存在对象"的沉默失败。
    /// 成功后移交聚合根中已经记录的领域事件,并立即清空。
    ///
    /// @param entity 聚合根
    void modify(T entity);

    /// 批量加入聚合集合(新建)。
    /// <p>
    /// 批量语义：逐个调用 {@link #add} 顺序执行。<b>本方法不提供事务边界</b>——
    /// 部分失败时已完成的写入不会回滚。
    /// <p>
    /// 事务边界归属应用层。如需原子性，调用方应在应用服务方法上显式标注 {@code @Transactional}，
    /// 并优先按 "一个事务修改一个聚合根" 的 DDD 原则拆分聚合边界。
    ///
    /// @param entities 聚合根集合
    void addAll(Collection<T> entities);

    /// 批量修改聚合集合中已存在的元素。
    /// <p>
    /// 批量语义：逐个调用 {@link #modify} 顺序执行。<b>本方法不提供事务边界</b>——
    /// 部分失败时已完成的写入不会回滚。任一元素受影响行数为 0 时抛出 {@link IllegalStateException}。
    /// 如需原子性，请在应用层显式管理事务。
    ///
    /// @param entities 聚合根集合
    void modifyAll(Collection<T> entities);

    /// 移除聚合。
    /// <p>
    /// 这是业务物理删除(或基于 @TableLogic 的逻辑删除)的推荐入口。调用方应先加载聚合根,并通过聚合业务方法表达删除语义、维护不变量和记录领域事件。
    /// Repository 只负责持久化移除。受影响行数为 0 时(即聚合不存在)抛出 {@link IllegalStateException},防御"删除了不存在对象"的沉默失败。
    /// 删除成功后移交聚合根中已经记录的领域事件,并立即清空。
    ///
    /// @param entity 聚合根
    void remove(T entity);
}
