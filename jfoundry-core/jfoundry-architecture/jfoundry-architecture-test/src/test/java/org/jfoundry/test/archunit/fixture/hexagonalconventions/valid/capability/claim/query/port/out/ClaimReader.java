package org.jfoundry.test.archunit.fixture.hexagonalconventions.valid.capability.claim.query.port.out;

import org.jfoundry.architecture.hexagonal.SecondaryPort;
import org.jfoundry.test.archunit.fixture.hexagonalconventions.valid.capability.claim.query.view.ClaimView;

@SecondaryPort
public interface ClaimReader {

    ClaimView find();
}
