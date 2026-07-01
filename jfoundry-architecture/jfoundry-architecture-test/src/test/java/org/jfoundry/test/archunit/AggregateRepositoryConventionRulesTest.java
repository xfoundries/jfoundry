package org.jfoundry.test.archunit;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AggregateRepositoryConventionRulesTest {

    private static final ClassFileImporter IMPORTER = new ClassFileImporter();

    @Test
    void exposesAggregateRepositoryConventionRules() {
        ArchRule[] rules = JFoundryRules.aggregateRepositoryConventions();
        assertThat(rules).hasSize(3);
        for (ArchRule rule : rules) {
            assertThat(rule).as("rule in JFoundryRules.aggregateRepositoryConventions() must not be null").isNotNull();
        }
    }

    @Test
    void aggregateRepositoriesMustNotExposeQueryConditionTypes() {
        assertThatThrownBy(() -> AggregateRepositoryConventionRules.aggregate_repositories_must_not_expose_query_condition_types
                .check(IMPORTER.importPackages("org.jfoundry.test.archunit.fixture.repositoryconventions.invalid.querycondition")))
                .isInstanceOf(AssertionError.class);

        AggregateRepositoryConventionRules.aggregate_repositories_must_not_expose_query_condition_types
                .check(IMPORTER.importPackages("org.jfoundry.test.archunit.fixture.repositoryconventions.valid"));
    }

    @Test
    void aggregateRepositoriesMustNotExposePagingTypes() {
        assertThatThrownBy(() -> AggregateRepositoryConventionRules.aggregate_repositories_must_not_expose_paging_types
                .check(IMPORTER.importPackages("org.jfoundry.test.archunit.fixture.repositoryconventions.invalid.paging")))
                .isInstanceOf(AssertionError.class);

        AggregateRepositoryConventionRules.aggregate_repositories_must_not_expose_paging_types
                .check(IMPORTER.importPackages("org.jfoundry.test.archunit.fixture.repositoryconventions.valid"));
    }

    @Test
    void aggregateRepositoriesMustNotExposePersistenceServiceTypes() {
        assertThatThrownBy(() -> AggregateRepositoryConventionRules.aggregate_repositories_must_not_expose_persistence_service_types
                .check(IMPORTER.importPackages("org.jfoundry.test.archunit.fixture.repositoryconventions.invalid.persistenceservice")))
                .isInstanceOf(AssertionError.class);

        AggregateRepositoryConventionRules.aggregate_repositories_must_not_expose_persistence_service_types
                .check(IMPORTER.importPackages("org.jfoundry.test.archunit.fixture.repositoryconventions.valid"));
    }
}
