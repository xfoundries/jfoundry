package org.jfoundry.test.archunit.fixture.hexagonalconventions.valid.capability.claim.query.port.in;

import org.jfoundry.architecture.hexagonal.PrimaryPort;
import org.jfoundry.test.archunit.fixture.hexagonalconventions.valid.capability.claim.query.view.ClaimView;

@PrimaryPort
public interface FindClaimUseCase {

    ClaimView find();
}
