package org.jfoundry.infrastructure.outbox.mybatis;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/// Outbox 表 MyBatis-Plus Mapper。
/// <p>
/// 仅继承 {@link BaseMapper} 标准能力（insert/selectById/updateById/selectPage/deleteByIds 等），
/// 不再包含任何 {@code @Update}/{@code @Select}/{@code @Delete} 自定义 SQL。
/// <p>
/// 设计原则：所有跨方言能力（分页、CRUD、条件构造）由 MyBatis-Plus + PaginationInnerInterceptor
/// 在运行时根据 {@link com.baomidou.mybatisplus.annotation.DbType} 生成对应方言 SQL，
/// 不在源码层为每种数据库（MySQL / H2 / 达梦 / PostgreSQL / Oracle / ...）维护独立的 SQL 副本。
/// <p>
/// 关键操作实现策略（均在 {@link MybatisPlusOutboxRepository}）：
/// <ul>
///   <li><b>原子批量 claim</b>：放弃 MySQL 特有的 {@code UPDATE...ORDER BY...LIMIT N}，
///       改为 {@code selectPage(N) → 逐条 CAS UPDATE}。selectPage 的 LIMIT 由 PaginationInnerInterceptor
///       按方言生成；CAS UPDATE 是标准 ANSI SQL（{@code WHERE event_id=? AND status=?}），
///       天然跨方言。CAS 失败的记录被并发 claimer 抢走，自然跳过，不发送。</li>
///   <li><b>stuck 恢复</b>：{@code lambdaUpdate().set(status, PENDING).setNull(claimedAt)...}，
///       标准条件 UPDATE。</li>
///   <li><b>批量清理</b>：{@code selectPage(N) + removeByIds} 循环，跨方言分页。</li>
/// </ul>
/// <p>
/// 实体类型为 {@link OutboxData}（MP 持久化视图），SPI 层 {@code OutboxEntry} 由
/// {@link MybatisPlusOutboxRepository} 在边界处互转。
@Mapper
public interface OutboxMapper extends BaseMapper<OutboxData> {
}
