package org.jfoundry.webmvc.spring;

import org.jfoundry.application.exception.ConflictException;
import org.jfoundry.application.exception.ExternalAccessException;
import org.jfoundry.application.exception.InvalidArgumentException;
import org.jfoundry.application.exception.NotFoundException;
import org.jfoundry.domain.exception.DomainRuleViolationException;
import org.jfoundry.domain.exception.DomainStateException;
import org.jfoundry.problem.ProblemCatalog;
import org.jfoundry.problem.ProblemDescriptor;
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

    @ExceptionHandler(InvalidArgumentException.class)
    public ResponseEntity<ProblemDetail> handleInvalidArgument(InvalidArgumentException exception) {
        return problem(ProblemCatalog.forException(exception));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(NotFoundException exception) {
        return problem(ProblemCatalog.forException(exception));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ProblemDetail> handleConflict(ConflictException exception) {
        return problem(ProblemCatalog.forException(exception));
    }

    @ExceptionHandler(ExternalAccessException.class)
    public ResponseEntity<ProblemDetail> handleExternalAccess(ExternalAccessException exception) {
        return problem(ProblemCatalog.forException(exception));
    }

    @ExceptionHandler(DomainRuleViolationException.class)
    public ResponseEntity<ProblemDetail> handleDomainRuleViolation(DomainRuleViolationException exception) {
        return problem(ProblemCatalog.forException(exception));
    }

    @ExceptionHandler(DomainStateException.class)
    public ResponseEntity<ProblemDetail> handleDomainState(DomainStateException exception) {
        return problem(ProblemCatalog.forException(exception));
    }

    private static ResponseEntity<ProblemDetail> problem(ProblemDescriptor descriptor) {
        return ResponseEntity.status(HttpStatusCode.valueOf(descriptor.status()))
                .body(toProblemDetail(descriptor));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception,
                                                             @Nullable Object body,
                                                             HttpHeaders headers,
                                                             HttpStatusCode statusCode,
                                                             WebRequest request) {
        ProblemDescriptor descriptor = ProblemCatalog.forHttpStatus(statusCode.value());
        if (statusCode.isSameCodeAs(HttpStatus.INTERNAL_SERVER_ERROR) && request instanceof ServletWebRequest) {
            request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, exception, WebRequest.SCOPE_REQUEST);
        }
        return super.handleExceptionInternal(exception, toProblemDetail(descriptor), headers,
                HttpStatusCode.valueOf(descriptor.status()), request);
    }

    private static ProblemDetail toProblemDetail(ProblemDescriptor descriptor) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(descriptor.status()),
                descriptor.detail());
        problemDetail.setTitle(descriptor.title());
        problemDetail.setType(descriptor.type());
        problemDetail.setProperty("code", descriptor.code());
        return problemDetail;
    }
}
