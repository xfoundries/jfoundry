package org.jfoundry.web.helidon;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.jfoundry.application.exception.ConflictException;
import org.jfoundry.application.exception.ExternalAccessException;
import org.jfoundry.application.exception.InvalidArgumentException;
import org.jfoundry.application.exception.NotFoundException;
import org.jfoundry.domain.exception.DomainRuleViolationException;
import org.jfoundry.domain.exception.DomainStateException;
import org.jfoundry.problem.ProblemCatalog;

/// Groups the precise Jakarta REST exception mappers used for JFoundry problem responses.
public final class ProblemDetailsExceptionMappers {

    private ProblemDetailsExceptionMappers() {
    }

    private abstract static class JFoundryExceptionMapper<E extends Exception> implements ExceptionMapper<E> {
        @Override
        public final Response toResponse(E exception) {
            return ProblemDetailsResponses.forException(exception);
        }
    }

    public static final class InvalidArgumentMapper extends JFoundryExceptionMapper<InvalidArgumentException> { }
    public static final class NotFoundMapper extends JFoundryExceptionMapper<NotFoundException> { }
    public static final class ConflictMapper extends JFoundryExceptionMapper<ConflictException> { }
    public static final class ExternalAccessMapper extends JFoundryExceptionMapper<ExternalAccessException> { }
    public static final class DomainRuleViolationMapper extends JFoundryExceptionMapper<DomainRuleViolationException> { }
    public static final class DomainStateMapper extends JFoundryExceptionMapper<DomainStateException> { }

    public static final class WebApplicationMapper implements ExceptionMapper<WebApplicationException> {
        @Override
        public Response toResponse(WebApplicationException exception) {
            Response source = exception.getResponse();
            return ProblemCatalog.supportsHttpStatus(source.getStatus())
                    ? ProblemDetailsResponses.forHttpStatus(source.getStatus(), source.getHeaders())
                    : source;
        }
    }
}
