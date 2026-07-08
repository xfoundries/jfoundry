package org.jfoundry.domain.entity;

import java.time.LocalDateTime;

/// Capability for deletable domain objects.
/// <p>
/// This interface only expresses soft-delete state and deletion audit metadata.
/// It does not bind domain objects to a persistence framework or field
/// annotations.
public interface Deletable {

    boolean isDeleted();

    LocalDateTime getDeletedTime();

    String getDeleterId();

    String getDeleterName();
}
