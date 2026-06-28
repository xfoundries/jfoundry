package org.jfoundry.test.archunit.fixture.hexagonalconventions.valid.adapter.in;

import org.jfoundry.architecture.hexagonal.PrimaryAdapter;
import org.jfoundry.test.archunit.fixture.hexagonalconventions.valid.port.in.CreateHelpDocumentUseCase;

@PrimaryAdapter
public class HelpDocumentController {

    private final CreateHelpDocumentUseCase useCase;

    public HelpDocumentController(CreateHelpDocumentUseCase useCase) {
        this.useCase = useCase;
    }

    void create() {
        useCase.create("title");
    }
}
