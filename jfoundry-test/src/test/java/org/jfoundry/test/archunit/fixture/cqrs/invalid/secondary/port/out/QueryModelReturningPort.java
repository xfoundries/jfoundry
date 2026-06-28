package org.jfoundry.test.archunit.fixture.cqrs.invalid.secondary.port.out;

import org.jfoundry.architecture.hexagonal.SecondaryPort;

import java.util.List;

@SecondaryPort
interface QueryModelReturningPort {

    List<OutboundQueryModel> findSummaries();
}
