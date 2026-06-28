package org.jfoundry.test.archunit.fixture.hexagonalconventions.invalid.primaryadapter.port.out;

import org.jfoundry.architecture.hexagonal.SecondaryPort;

@SecondaryPort
public interface HelpDocumentReadModelPort {

    String findTitle();
}
