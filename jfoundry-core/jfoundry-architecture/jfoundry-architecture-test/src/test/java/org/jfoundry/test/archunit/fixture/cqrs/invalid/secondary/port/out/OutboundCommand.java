package org.jfoundry.test.archunit.fixture.cqrs.invalid.secondary.port.out;

import org.jfoundry.architecture.cqrs.Command;

@Command
record OutboundCommand(String title) {
}
