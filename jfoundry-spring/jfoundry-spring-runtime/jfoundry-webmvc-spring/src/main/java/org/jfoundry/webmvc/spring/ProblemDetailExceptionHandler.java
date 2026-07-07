package org.jfoundry.webmvc.spring;

import org.jfoundry.application.exception.ConflictException;
import org.jfoundry.application.exception.ExternalAccessException;
import org.jfoundry.application.exception.InvalidArgumentException;
import org.jfoundry.application.exception.NotFoundException;
import org.jfoundry.domain.exception.DomainRuleViolationException;
import org.jfoundry.domain.exception.DomainStateException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.util.WebUtils;

/**
 * Maps jfoundry core exceptions to Spring MVC RFC 9457 ProblemDetail responses.
 */
@RestControllerAdvice
public class ProblemDetailExceptionHandler extends ResponseEntityExceptionHandler {

    private final CoreProblemCodeResolver coreProblemCodeResolver;
    private final HttpProblemCodeResolver httpProblemCodeResolver;

    public ProblemDetailExceptionHandler() {
        this(new CoreProblemCodeResolver(), new HttpProblemCodeResolver());
    }

    ProblemDetailExceptionHandler(CoreProblemCodeResolver coreProblemCodeResolver,
                                  HttpProblemCodeResolver httpProblemCodeResolver) {
        this.coreProblemCodeResolver = coreProblemCodeResolver;
        this.httpProblemCodeResolver = httpProblemCodeResolver;
    }

    @ExceptionHandler(InvalidArgumentException.class)
    public ResponseEntity<ProblemDetail> handleInvalidArgument(InvalidArgumentException exception) {
        return problem(coreProblemCodeResolver.resolve(exception), exception.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(NotFoundException exception) {
        return problem(coreProblemCodeResolver.resolve(exception), exception.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ProblemDetail> handleConflict(ConflictException exception) {
        return problem(coreProblemCodeResolver.resolve(exception), exception.getMessage());
    }

    @ExceptionHandler(ExternalAccessException.class)
    public ResponseEntity<ProblemDetail> handleExternalAccess(ExternalAccessException exception) {
        CoreProblemCode code = coreProblemCodeResolver.resolve(exception);
        return ResponseEntity.status(code.status())
                .body(ProblemDetails.create(code));
    }

    @ExceptionHandler(DomainRuleViolationException.class)
    public ResponseEntity<ProblemDetail> handleDomainRuleViolation(DomainRuleViolationException exception) {
        return problem(coreProblemCodeResolver.resolve(exception), exception.getMessage());
    }

    @ExceptionHandler(DomainStateException.class)
    public ResponseEntity<ProblemDetail> handleDomainState(DomainStateException exception) {
        return problem(coreProblemCodeResolver.resolve(exception), exception.getMessage());
    }

    private static ResponseEntity<ProblemDetail> problem(ProblemCode code, String detail) {
        return ResponseEntity.status(code.status())
                .body(ProblemDetails.create(code, detail));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception,
                                                             @Nullable Object body,
                                                             HttpHeaders headers,
                                                             HttpStatusCode statusCode,
                                                             WebRequest request) {
        HttpProblemCode code = httpProblemCodeResolver.resolve(exception, statusCode);
        if (statusCode.isSameCodeAs(HttpStatus.INTERNAL_SERVER_ERROR) && request instanceof ServletWebRequest) {
            request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, exception, WebRequest.SCOPE_REQUEST);
        }
        return super.handleExceptionInternal(exception, ProblemDetails.create(code), headers, code.status(), request);
    }
}
