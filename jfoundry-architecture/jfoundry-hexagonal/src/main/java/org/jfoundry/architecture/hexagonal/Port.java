package org.jfoundry.architecture.hexagonal;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Marks a port through which the application core interacts with the outside world.
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PACKAGE, ElementType.TYPE})
@Documented
@org.jmolecules.architecture.hexagonal.Port
public @interface Port {
}
