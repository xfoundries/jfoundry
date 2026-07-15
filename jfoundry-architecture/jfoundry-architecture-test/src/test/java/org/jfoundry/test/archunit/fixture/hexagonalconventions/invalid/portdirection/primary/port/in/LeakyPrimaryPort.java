package org.jfoundry.test.archunit.fixture.hexagonalconventions.invalid.portdirection.primary.port.in;

import org.jfoundry.architecture.hexagonal.PrimaryPort;
import org.jfoundry.test.archunit.fixture.hexagonalconventions.invalid.portdirection.primary.port.out.OutboundView;

@PrimaryPort
public interface LeakyPrimaryPort {

    OutboundView load();
}
