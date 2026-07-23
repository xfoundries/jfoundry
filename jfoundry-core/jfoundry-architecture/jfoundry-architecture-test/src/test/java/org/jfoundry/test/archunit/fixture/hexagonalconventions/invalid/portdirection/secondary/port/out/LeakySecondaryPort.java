package org.jfoundry.test.archunit.fixture.hexagonalconventions.invalid.portdirection.secondary.port.out;

import org.jfoundry.architecture.hexagonal.SecondaryPort;
import org.jfoundry.test.archunit.fixture.hexagonalconventions.invalid.portdirection.secondary.port.in.InboundRequest;

@SecondaryPort
public interface LeakySecondaryPort {

    void store(InboundRequest request);
}
