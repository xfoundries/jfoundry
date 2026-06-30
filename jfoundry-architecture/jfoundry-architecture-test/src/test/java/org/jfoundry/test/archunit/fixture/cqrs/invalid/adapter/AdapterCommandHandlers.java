package org.jfoundry.test.archunit.fixture.cqrs.invalid.adapter;

import org.jfoundry.architecture.cqrs.CommandDispatcher;
import org.jfoundry.architecture.cqrs.CommandHandler;

class AdapterCommandHandlers {

    @CommandHandler
    void handle() {
    }

    @CommandDispatcher
    void dispatch() {
    }
}
