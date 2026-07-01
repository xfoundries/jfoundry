package org.jfoundry.test.archunit.fixture.repositoryconventions.valid;

import org.jfoundry.domain.repository.AggregateRepository;

import java.util.List;

public interface HelpDocumentRepository extends AggregateRepository<HelpDocument, HelpDocumentId> {

    HelpDocument findPublishedBySlug(String slug);

    List<HelpDocument> findDocumentsPendingReview();
}
