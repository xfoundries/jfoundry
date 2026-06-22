package org.jfoundry.domain.entity.agg;

import org.jfoundry.domain.entity.Auditable;
import org.jfoundry.domain.entity.Deletable;
import org.jmolecules.ddd.types.AggregateRoot;
import org.jmolecules.ddd.types.Identifier;

import java.time.LocalDateTime;

/// 可审计的聚合根便利基类。
///
/// 该类提供固定审计字段。字段写入通过受保护方法和意图方法暴露给子类，业务代码不应把它当作通用数据对象 setter 使用。
///
/// @param <T>  聚合根本类型（self type，与 {@link BaseAggregateRoot} 对齐）
/// @param <ID> 标识符类型
/// @see BaseAggregateRoot
/// @see AggregateRoot
///
public abstract class AuditableAggregateRoot<T extends AggregateRoot<T, ID>, ID extends Identifier>
        extends BaseAggregateRoot<T, ID>
        implements Auditable, Deletable {

    /// 创建人ID
    private String creatorId;

    /// 创建人姓名
    private String creatorName;

    /// 创建时间
    private LocalDateTime createdTime;

    /// 最后修改人ID
    private String lastModifierId;

    /// 最后修改人姓名
    private String lastModifierName;

    /// 最后修改时间
    private LocalDateTime lastModifiedTime;

    /// 是否已删除（软删除标记）
    private boolean deleted;
    /// 删除时间
    private LocalDateTime deletedTime;
    /// 删除人ID
    private String deleterId;

    /// 删除人姓名
    private String deleterName;

    public AuditableAggregateRoot(ID id) {
        super(id);
    }

    @Override
    public String getCreatorId() {
        return creatorId;
    }

    @Override
    public String getCreatorName() {
        return creatorName;
    }

    @Override
    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    @Override
    public String getLastModifierId() {
        return lastModifierId;
    }

    @Override
    public String getLastModifierName() {
        return lastModifierName;
    }

    @Override
    public LocalDateTime getLastModifiedTime() {
        return lastModifiedTime;
    }

    @Override
    public boolean isDeleted() {
        return deleted;
    }

    @Override
    public LocalDateTime getDeletedTime() {
        return deletedTime;
    }

    @Override
    public String getDeleterId() {
        return deleterId;
    }

    @Override
    public String getDeleterName() {
        return deleterName;
    }

    /// 标记创建审计信息。
    protected void markCreated(String creatorId, String creatorName, LocalDateTime createdTime) {
        this.creatorId = creatorId;
        this.creatorName = creatorName;
        this.createdTime = createdTime;
    }

    /// 标记修改审计信息。
    protected void markModified(String modifierId, String modifierName, LocalDateTime modifiedTime) {
        this.lastModifierId = modifierId;
        this.lastModifierName = modifierName;
        this.lastModifiedTime = modifiedTime;
    }

    /// 标记软删除审计信息。
    protected void markDeleted(String deleterId, String deleterName, LocalDateTime deletedTime) {
        this.deleted = true;
        this.deleterId = deleterId;
        this.deleterName = deleterName;
        this.deletedTime = deletedTime;
    }

    /// 清除软删除审计信息。
    protected void restore() {
        this.deleted = false;
        this.deleterId = null;
        this.deleterName = null;
        this.deletedTime = null;
    }

}
