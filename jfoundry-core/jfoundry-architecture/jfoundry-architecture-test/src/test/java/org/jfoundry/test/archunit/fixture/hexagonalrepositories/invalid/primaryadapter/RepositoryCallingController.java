package org.jfoundry.test.archunit.fixture.hexagonalrepositories.invalid.primaryadapter;

import org.jfoundry.architecture.hexagonal.PrimaryAdapter;
import org.jfoundry.test.archunit.fixture.hexagonalrepositories.valid.domain.repository.HelpDocumentRepository;

@PrimaryAdapter
public final class RepositoryCallingController {

    private final HelpDocumentRepository repository;

    public RepositoryCallingController(HelpDocumentRepository repository) {
        this.repository = repository;
    }
}
