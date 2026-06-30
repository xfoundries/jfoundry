package org.jfoundry.test.archunit.fixture.hexagonalconventions.valid.port.out;

import org.jfoundry.architecture.hexagonal.SecondaryPort;

@SecondaryPort
public interface HelpDocumentReadModelPort {

    String findTitle();
}
