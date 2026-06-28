package org.jfoundry.test.archunit.fixture.hexagonalconventions.valid.port.in;

import org.jfoundry.architecture.hexagonal.PrimaryPort;

@PrimaryPort
public interface CreateHelpDocumentUseCase {

    void create(String title);
}
