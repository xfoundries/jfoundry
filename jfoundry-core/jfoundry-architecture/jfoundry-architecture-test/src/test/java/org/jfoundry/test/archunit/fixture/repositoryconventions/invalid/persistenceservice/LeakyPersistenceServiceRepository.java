package org.jfoundry.test.archunit.fixture.repositoryconventions.invalid.persistenceservice;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jfoundry.domain.repository.AggregateRepository;
import org.jfoundry.test.archunit.fixture.repositoryconventions.valid.HelpDocument;
import org.jfoundry.test.archunit.fixture.repositoryconventions.valid.HelpDocumentId;

public interface LeakyPersistenceServiceRepository extends AggregateRepository<HelpDocument, HelpDocumentId> {

    IService<HelpDocument> service();
}
