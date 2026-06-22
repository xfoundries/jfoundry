package org.jfoundry.domain.entity;

import org.jfoundry.domain.entity.agg.AuditableAggregateRoot;
import org.jmolecules.ddd.types.Identifier;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditableModelTest {

    @Test
    void auditableAggregateRootImplementsAuditCapabilities() {
        var aggregate = new TestAggregateRoot(new TestId("root-1"));

        assertInstanceOf(Auditable.class, aggregate);
        assertInstanceOf(Deletable.class, aggregate);
    }

    @Test
    void auditableEntityImplementsAuditCapabilities() {
        var entity = new TestEntity(new TestId("entity-1"));

        assertInstanceOf(Auditable.class, entity);
        assertInstanceOf(Deletable.class, entity);
    }

    @Test
    void customModelCanImplementAuditCapabilitiesWithoutBaseClass() {
        var model = new CustomAuditModel();

        assertTrue(model instanceof Auditable);
        assertTrue(model instanceof Deletable);
    }

    record TestId(String value) implements Identifier {
    }

    private static class TestAggregateRoot extends AuditableAggregateRoot<TestAggregateRoot, TestId> {
        private TestAggregateRoot(TestId id) {
            super(id);
        }
    }

    private static class TestEntity extends AuditableEntity<TestAggregateRoot, TestId> {
        private TestEntity(TestId id) {
            super(id);
        }
    }

    private static class CustomAuditModel implements Auditable, Deletable {
        public String getId() {
            return "custom-1";
        }

        @Override
        public String getCreatorId() {
            return "creator-1";
        }

        @Override
        public String getCreatorName() {
            return "creator";
        }

        @Override
        public LocalDateTime getCreatedTime() {
            return LocalDateTime.MIN;
        }

        @Override
        public String getLastModifierId() {
            return "modifier-1";
        }

        @Override
        public String getLastModifierName() {
            return "modifier";
        }

        @Override
        public LocalDateTime getLastModifiedTime() {
            return LocalDateTime.MAX;
        }

        @Override
        public boolean isDeleted() {
            return false;
        }

        @Override
        public LocalDateTime getDeletedTime() {
            return null;
        }

        @Override
        public String getDeleterId() {
            return null;
        }

        @Override
        public String getDeleterName() {
            return null;
        }
    }
}
