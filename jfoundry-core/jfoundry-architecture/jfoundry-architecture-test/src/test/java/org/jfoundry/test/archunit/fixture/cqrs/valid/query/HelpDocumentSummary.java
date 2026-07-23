package org.jfoundry.test.archunit.fixture.cqrs.valid.query;

import org.jfoundry.architecture.cqrs.QueryModel;

@QueryModel
record HelpDocumentSummary(String title) {
}
