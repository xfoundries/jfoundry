package org.jfoundry.infrastructure.messaging.mybatis.outbox;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/// Outbox 表 MyBatis-Plus Mapper。
/// <p>
/// 仅暴露 BaseMapper 标准能力（insert/selectById/updateById 等），不附加自定义 SQL。
/// findDispatchable 由 MybatisPlusOutboxRepository 通过 Wrappers 表达。
/// <p>
/// 实体类型为 {@link OutboxData}（MP 持久化视图），SPI 层 {@code OutboxEntry} 由
/// {@link MybatisPlusOutboxRepository} 在边界处互转。
@Mapper
public interface OutboxMapper extends BaseMapper<OutboxData> {
}
