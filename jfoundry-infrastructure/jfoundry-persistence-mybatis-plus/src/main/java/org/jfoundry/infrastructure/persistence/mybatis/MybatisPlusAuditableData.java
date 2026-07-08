package org.jfoundry.infrastructure.persistence.mybatis;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import org.jfoundry.infrastructure.persistence.AggregateData;

import java.io.Serializable;
import java.time.LocalDateTime;

/// Auditable MyBatis-Plus data base class.
/// Extends AggregateData with auditing fields.
/// <p>
/// This is a fixed-field convenience base class provided by the MyBatis-Plus adapter. Non-standard
/// aggregate field models can extend AggregateData directly and implement Auditable/Deletable
/// capability interfaces on the domain object.
/// <p>
/// The primary-key field {@code id} is provided by the parent AggregateData. MyBatis-Plus recognizes
/// a field named {@code id} as the primary key by default, so neither this class nor its parent needs
/// an explicit {@code @TableId}. If a custom key strategy is required, such as {@code IdType.AUTO},
/// application subclasses can redeclare the field and annotate it with {@code @TableId}.
/// <p>
/// hashCode, equals, and toString are inherited from the parent and are based on ID, not auditing
/// fields, preventing distinct non-persisted objects from being folded together because of auditing
/// field differences.
///
/// @param <ID> persistence identifier type, which must be serializable
public abstract class MybatisPlusAuditableData<ID extends Serializable> extends AggregateData<ID> {

    /// Creator ID.
    @TableField(fill = FieldFill.INSERT)
    private String creatorId;

    /// Creator name.
    @TableField(fill = FieldFill.INSERT)
    private String creatorName;

    /// Creation time.
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    /// Last modifier ID.
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String lastModifierId;

    /// Last modifier name.
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String lastModifierName;

    /// Last modification time.
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime lastModifiedTime;

    /// Soft-delete flag.
    @TableField(value = "is_deleted")
    @TableLogic
    private boolean deleted;

    /// Deletion time.
    private LocalDateTime deletedTime;

    /// Deleter ID.
    private String deleterId;

    /// Deleter name.
    private String deleterName;

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public String getLastModifierId() {
        return lastModifierId;
    }

    public void setLastModifierId(String lastModifierId) {
        this.lastModifierId = lastModifierId;
    }

    public String getLastModifierName() {
        return lastModifierName;
    }

    public void setLastModifierName(String lastModifierName) {
        this.lastModifierName = lastModifierName;
    }

    public LocalDateTime getLastModifiedTime() {
        return lastModifiedTime;
    }

    public void setLastModifiedTime(LocalDateTime lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public LocalDateTime getDeletedTime() {
        return deletedTime;
    }

    public void setDeletedTime(LocalDateTime deletedTime) {
        this.deletedTime = deletedTime;
    }

    public String getDeleterId() {
        return deleterId;
    }

    public void setDeleterId(String deleterId) {
        this.deleterId = deleterId;
    }

    public String getDeleterName() {
        return deleterName;
    }

    public void setDeleterName(String deleterName) {
        this.deleterName = deleterName;
    }
}
