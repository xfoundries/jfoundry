package org.jfoundry.test.archunit.fixture.repositoryconventions.invalid.jmoleculespaging;

import org.jfoundry.test.archunit.fixture.hexagonalrepositories.valid.domain.model.HelpDocument;
import org.jfoundry.test.archunit.fixture.hexagonalrepositories.valid.domain.model.HelpDocumentId;
import org.jmolecules.ddd.types.Repository;
import org.springframework.data.domain.Page;

public interface JmoleculesLeakyPagingRepository extends Repository<HelpDocument, HelpDocumentId> {

    Page<HelpDocument> findAll();
}
