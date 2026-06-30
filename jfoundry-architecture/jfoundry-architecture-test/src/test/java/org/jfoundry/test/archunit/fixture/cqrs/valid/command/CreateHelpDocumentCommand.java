package org.jfoundry.test.archunit.fixture.cqrs.valid.command;

import org.jfoundry.architecture.cqrs.Command;

@Command
record CreateHelpDocumentCommand(String title) {
}
