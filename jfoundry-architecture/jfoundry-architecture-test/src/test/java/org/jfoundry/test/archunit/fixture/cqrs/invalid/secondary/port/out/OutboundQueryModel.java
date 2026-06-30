package org.jfoundry.test.archunit.fixture.cqrs.invalid.secondary.port.out;

import org.jfoundry.architecture.cqrs.QueryModel;

@QueryModel
record OutboundQueryModel(String title) {
}
