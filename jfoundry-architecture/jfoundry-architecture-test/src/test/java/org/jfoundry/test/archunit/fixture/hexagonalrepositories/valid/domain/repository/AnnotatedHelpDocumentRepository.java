package org.jfoundry.test.archunit.fixture.hexagonalrepositories.valid.domain.repository;

import org.jfoundry.architecture.hexagonal.SecondaryPort;
import org.jfoundry.test.archunit.fixture.hexagonalrepositories.valid.domain.model.HelpDocument;
import org.jfoundry.test.archunit.fixture.hexagonalrepositories.valid.domain.model.HelpDocumentId;
import org.jmolecules.ddd.types.Repository;

@SecondaryPort
public interface AnnotatedHelpDocumentRepository extends Repository<HelpDocument, HelpDocumentId> {
}
