package org.jfoundry.architecture.layered;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Marks application-layer code. Apply this annotation to {@code package-info.java}
/// to declare the whole package as part of the application layer.
/// <p>
/// The application layer orchestrates domain objects to execute use cases,
/// manages transaction boundaries, and declares domain event consumption.
/// It coordinates behavior but does not own business rules; business rules
/// belong in the domain layer.
/// <p>
/// Dependency direction: application layer to domain layer. Application code
/// must not depend on adapter or infrastructure layers.
@org.jmolecules.architecture.layered.ApplicationLayer
@Target({ElementType.PACKAGE, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApplicationLayer {
}
