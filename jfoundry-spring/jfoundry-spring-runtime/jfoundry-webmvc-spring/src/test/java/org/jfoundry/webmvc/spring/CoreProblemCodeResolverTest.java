package org.jfoundry.webmvc.spring;

import org.jfoundry.application.exception.ExternalAccessException;
import org.jfoundry.application.exception.InvalidArgumentException;
import org.jfoundry.domain.exception.DomainRuleViolationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CoreProblemCodeResolverTest {

    private final CoreProblemCodeResolver resolver = new CoreProblemCodeResolver();

    @Test
    void resolvesApplicationExceptionsToCoreProblemCodes() {
        assertThat(resolver.resolve(new InvalidArgumentException("Invalid page size")))
                .isEqualTo(CoreProblemCode.INVALID_ARGUMENT);
        assertThat(resolver.resolve(new ExternalAccessException("k8s timeout")))
                .isEqualTo(CoreProblemCode.EXTERNAL_ACCESS);
    }

    @Test
    void resolvesDomainExceptionsToCoreProblemCodes() {
        assertThat(resolver.resolve(new DomainRuleViolationException("Quota exceeded")))
                .isEqualTo(CoreProblemCode.DOMAIN_RULE_VIOLATION);
    }
}
