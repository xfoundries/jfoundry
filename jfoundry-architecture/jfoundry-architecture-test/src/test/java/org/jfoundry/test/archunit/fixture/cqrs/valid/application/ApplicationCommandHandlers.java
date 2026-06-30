package org.jfoundry.test.archunit.fixture.cqrs.valid.application;

import org.jfoundry.architecture.cqrs.CommandDispatcher;
import org.jfoundry.architecture.cqrs.CommandHandler;

class ApplicationCommandHandlers {

    @CommandHandler
    void handle() {
    }

    @CommandDispatcher
    void dispatch() {
    }
}
