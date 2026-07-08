package org.jfoundry.domain.entity;

import java.time.LocalDateTime;

/// Capability for auditable domain objects.
/// <p>
/// This interface only expresses creation and modification audit metadata. It
/// does not force objects to inherit a fixed field base class. Implement this
/// interface directly when using non-standard fields, external audit components,
/// or custom persistence models.
public interface Auditable {

    String getCreatorId();

    String getCreatorName();

    LocalDateTime getCreatedTime();

    String getLastModifierId();

    String getLastModifierName();

    LocalDateTime getLastModifiedTime();
}
