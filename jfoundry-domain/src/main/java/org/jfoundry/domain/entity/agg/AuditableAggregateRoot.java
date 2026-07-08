package org.jfoundry.domain.entity.agg;

import org.jfoundry.domain.entity.Auditable;
import org.jfoundry.domain.entity.Deletable;
import org.jmolecules.ddd.types.AggregateRoot;
import org.jmolecules.ddd.types.Identifier;

import java.time.LocalDateTime;

/// Convenience base class for auditable aggregate roots.
///
/// This class provides fixed audit fields. Field mutation is exposed to
/// subclasses through protected intent methods; business code should not treat
/// it as a generic data-object setter surface.
///
/// @param <T> aggregate root self type aligned with {@link BaseAggregateRoot}
/// @param <ID> identifier type
/// @see BaseAggregateRoot
/// @see AggregateRoot
///
public abstract class AuditableAggregateRoot<T extends AggregateRoot<T, ID>, ID extends Identifier>
        extends BaseAggregateRoot<T, ID>
        implements Auditable, Deletable {

    /// Creator id.
    private String creatorId;

    /// Creator name.
    private String creatorName;

    /// Creation time.
    private LocalDateTime createdTime;

    /// Last modifier id.
    private String lastModifierId;

    /// Last modifier name.
    private String lastModifierName;

    /// Last modification time.
    private LocalDateTime lastModifiedTime;

    /// Soft-delete flag.
    private boolean deleted;
    /// Deletion time.
    private LocalDateTime deletedTime;
    /// Deleter id.
    private String deleterId;

    /// Deleter name.
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

    /// Marks soft-delete audit metadata.
    protected void markDeleted(String deleterId, String deleterName, LocalDateTime deletedTime) {
        this.deleted = true;
        this.deleterId = deleterId;
        this.deleterName = deleterName;
        this.deletedTime = deletedTime;
    }

    /// Clears soft-delete audit metadata.
    protected void restore() {
        this.deleted = false;
        this.deleterId = null;
        this.deleterName = null;
        this.deletedTime = null;
    }

}
