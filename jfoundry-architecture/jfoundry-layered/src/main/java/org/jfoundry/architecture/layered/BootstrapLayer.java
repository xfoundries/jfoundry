package org.jfoundry.architecture.layered;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Marks bootstrap and assembly code. Apply this annotation to
/// {@code package-info.java} to declare the whole package as part of the
/// framework bootstrap layer.
/// <p>
/// The bootstrap layer contains auto-configuration, conditional assembly,
/// properties binding, runtime module activation, and framework glue code.
/// <p>
/// Dependency direction: the bootstrap layer is an outer layer. It may depend
/// on application contracts and infrastructure implementations, while domain
/// and application layers must not depend on it.
@Target({ElementType.PACKAGE, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface BootstrapLayer {
}
