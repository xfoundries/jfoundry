package org.jfoundry.webmvc.spring;

import org.jfoundry.application.exception.ConflictException;
import org.jfoundry.application.exception.ExternalAccessException;
import org.jfoundry.application.exception.InvalidArgumentException;
import org.jfoundry.application.exception.NotFoundException;
import org.jfoundry.domain.exception.DomainRuleViolationException;
import org.jfoundry.domain.exception.DomainStateException;

/**
 * Resolves jfoundry application and domain exceptions to core ProblemDetail codes.
 */
public class CoreProblemCodeResolver {

    public CoreProblemCode resolve(Exception exception) {
        if (exception instanceof InvalidArgumentException) {
            return CoreProblemCode.INVALID_ARGUMENT;
        }
        if (exception instanceof NotFoundException) {
            return CoreProblemCode.NOT_FOUND;
        }
        if (exception instanceof ConflictException) {
            return CoreProblemCode.CONFLICT;
        }
        if (exception instanceof ExternalAccessException) {
            return CoreProblemCode.EXTERNAL_ACCESS;
        }
        if (exception instanceof DomainRuleViolationException) {
            return CoreProblemCode.DOMAIN_RULE_VIOLATION;
        }
        if (exception instanceof DomainStateException) {
            return CoreProblemCode.DOMAIN_STATE;
        }
        throw new IllegalArgumentException("Unsupported core exception: " + exception.getClass().getName());
    }
}
