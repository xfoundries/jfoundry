package org.jfoundry.infrastructure.persistence.mybatis;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.jfoundry.domain.event.DomainEventPublisher;
import org.jfoundry.domain.event.EventRecordable;
import org.jfoundry.infrastructure.persistence.AbstractPersistenceRepository;
import org.jfoundry.infrastructure.persistence.BaseData;
import org.jfoundry.infrastructure.persistence.DataConverter;
import org.jmolecules.ddd.types.AggregateRoot;
import org.jmolecules.ddd.types.Identifier;

import java.io.Serializable;

/// MyBatis-Plus 仓储实现(基于 AbstractPersistenceRepository 模板方法基类)。
/// <p>
/// 本类只实现 4 个模板方法(insertData/updateData/deleteDataById/selectDataById),
/// 聚合生命周期、事件移交、modify/remove 0 行防御等骨架逻辑由父类提供。
/// <p>
/// 条件查询、统计、分页和维护删除应由具体业务边界表达,在子类中使用 MyBatis 原生能力实现。
///
/// @param <T>  聚合根类型，必须同时是 jMolecules AggregateRoot 和 framework EventRecordable
/// @param <ID> 标识符类型
/// @param <D>  数据库实体类型
public abstract class MybatisPlusRepository<
        T extends AggregateRoot<T, ID> & EventRecordable,
        ID extends Identifier & Serializable,
        D extends BaseData<ID>>
        extends AbstractPersistenceRepository<T, ID, D> {

    protected final BaseMapper<D> mapper;

    protected MybatisPlusRepository(BaseMapper<D> mapper,
                                     DomainEventPublisher eventPublisher,
                                     DataConverter<T, ID, D> converter) {
        super(eventPublisher, converter);
        this.mapper = mapper;
    }

    @Override
    protected void insertData(D data) {
        mapper.insert(data);
    }

    @Override
    protected long updateData(D data) {
        return mapper.updateById(data);
    }

    @Override
    protected long deleteDataById(ID id) {
        return mapper.deleteById(id);
    }

    @Override
    protected D selectDataById(ID id) {
        return mapper.selectById(id);
    }
}
