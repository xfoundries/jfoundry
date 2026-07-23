package org.jfoundry.domain.entity;

import org.jmolecules.ddd.types.AggregateRoot;
import org.jmolecules.ddd.types.Identifier;

import java.time.LocalDateTime;

/// Base class for auditable entities that live inside an aggregate.
///
/// Provides standard audit fields. Mutators are protected so subclasses can expose intention-revealing
/// behavior instead of treating the entity as a generic data object.
///
/// @param <T>  aggregate root type that owns this entity
/// @param <ID> entity identifier type, independent from the aggregate root identifier type
/// @see org.jfoundry.domain.entity.agg.AuditableAggregateRoot
///
public abstract class AuditableEntity<T extends AggregateRoot<T, ?>, ID extends Identifier>
        extends BaseEntity<T, ID>
        implements Auditable, Deletable {

    /// Creator identifier.
    private String creatorId;

    /// Creator name.
    private String creatorName;

    /// Creation time.
    private LocalDateTime createdTime;

    /// Last modifier identifier.
    private String lastModifierId;

    /// Last modifier name.
    private String lastModifierName;

    /// Last modification time.
    private LocalDateTime lastModifiedTime;

    /// Soft-delete flag.
    private boolean deleted;

    /// Deletion time.
    private LocalDateTime deletedTime;

    /// Deleter identifier.
    private String deleterId;

    /// Deleter name.
    private String deleterName;

    public AuditableEntity(ID id) {
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

    /// Marks creation audit metadata.
    protected void markCreated(String creatorId, String creatorName, LocalDateTime createdTime) {
        this.creatorId = creatorId;
        this.creatorName = creatorName;
        this.createdTime = createdTime;
    }

    /// Marks modification audit metadata.
    protected void markModified(String modifierId, String modifierName, LocalDateTime modifiedTime) {
        this.lastModifierId = modifierId;
        this.lastModifierName = modifierName;
        this.lastModifiedTime = modifiedTime;
    }

    /// Marks soft-deletion audit metadata.
    protected void markDeleted(String deleterId, String deleterName, LocalDateTime deletedTime) {
        this.deleted = true;
        this.deleterId = deleterId;
        this.deleterName = deleterName;
        this.deletedTime = deletedTime;
    }

    /// Clears soft-deletion audit metadata.
    protected void restore() {
        this.deleted = false;
        this.deleterId = null;
        this.deleterName = null;
        this.deletedTime = null;
    }

}
