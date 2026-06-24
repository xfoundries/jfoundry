package org.jfoundry.architecture.layered;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Marks interface-layer or adapter-layer code. Apply this annotation to
/// {@code package-info.java} to declare the whole package as part of the
/// interface layer.
/// <p>
/// The interface layer contains components that receive external input, such as
/// REST controllers, MVC controllers, MQTT or message listeners, scheduled
/// jobs, and gRPC service implementations.
/// <p>
/// Dependency direction: interface layer to application layer. Interface code
/// should not directly call the domain layer or infrastructure layer.
@org.jmolecules.architecture.layered.InterfaceLayer
@Target({ElementType.PACKAGE, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface InterfaceLayer {
}
