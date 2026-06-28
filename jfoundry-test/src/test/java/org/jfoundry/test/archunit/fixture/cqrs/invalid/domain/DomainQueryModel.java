package org.jfoundry.test.archunit.fixture.cqrs.invalid.domain;

import org.jfoundry.architecture.cqrs.QueryModel;

@QueryModel
record DomainQueryModel(String title) {
}
