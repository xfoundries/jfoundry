package org.jfoundry.test.archunit.fixture.cqrs.invalid.command;

import org.jfoundry.architecture.cqrs.Command;
import org.jfoundry.test.archunit.fixture.cqrs.invalid.infrastructure.InfrastructureDependency;

@Command
record InfrastructureAwareCommand(InfrastructureDependency dependency) {
}
