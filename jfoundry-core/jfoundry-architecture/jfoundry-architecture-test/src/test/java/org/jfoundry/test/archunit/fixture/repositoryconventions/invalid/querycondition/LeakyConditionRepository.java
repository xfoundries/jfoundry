package org.jfoundry.test.archunit.fixture.repositoryconventions.invalid.querycondition;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.jfoundry.domain.repository.AggregateRepository;
import org.jfoundry.test.archunit.fixture.repositoryconventions.valid.HelpDocument;
import org.jfoundry.test.archunit.fixture.repositoryconventions.valid.HelpDocumentId;

import java.util.List;

public interface LeakyConditionRepository extends AggregateRepository<HelpDocument, HelpDocumentId> {

    List<HelpDocument> list(Wrapper<HelpDocument> wrapper);
}
