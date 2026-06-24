package org.jfoundry.architecture.layered;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Marks domain-layer code. Apply this annotation to {@code package-info.java}
/// to declare the whole package as part of the domain layer.
/// <p>
/// The domain layer contains aggregate roots, entities, value objects, domain
/// services, domain events, and repository interfaces or ports. It is the
/// business core and must stay free of framework and infrastructure dependencies.
@org.jmolecules.architecture.layered.DomainLayer
@Target({ElementType.PACKAGE, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DomainLayer {
}
