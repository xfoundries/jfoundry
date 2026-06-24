package org.jfoundry.architecture.layered;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Marks infrastructure-layer code. Apply this annotation to {@code package-info.java}
/// to declare the whole package as part of the infrastructure layer.
/// <p>
/// The infrastructure layer contains repository implementations, external
/// service client adapters, messaging adapters, and technical infrastructure.
/// <p>
/// Dependency direction: infrastructure layer to domain layer, usually by
/// implementing ports declared by inner layers. Interface and application code
/// should not directly depend on infrastructure implementations.
@org.jmolecules.architecture.layered.InfrastructureLayer
@Target({ElementType.PACKAGE, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface InfrastructureLayer {
}
