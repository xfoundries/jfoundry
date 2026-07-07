package org.jfoundry.webmvc.spring;

import org.springframework.http.HttpStatus;

/**
 * ProblemDetail codes for jfoundry application and domain exceptions.
 */
public enum CoreProblemCode implements ProblemCode {

    INVALID_ARGUMENT(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", "Invalid argument", "invalid-argument",
            "The request contains an invalid argument."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "Not found", "not-found", "The requested resource was not found."),
    CONFLICT(HttpStatus.CONFLICT, "CONFLICT", "Conflict", "conflict", "The request conflicts with current state."),
    EXTERNAL_ACCESS(HttpStatus.SERVICE_UNAVAILABLE, "EXTERNAL_ACCESS", "Service temporarily unavailable",
            "external-access", "The requested operation is temporarily unavailable."),
    DOMAIN_RULE_VIOLATION(HttpStatus.UNPROCESSABLE_ENTITY, "DOMAIN_RULE_VIOLATION", "Domain rule violation",
            "domain-rule-violation", "The request violates a domain rule."),
    DOMAIN_STATE(HttpStatus.CONFLICT, "DOMAIN_STATE", "Domain state conflict", "domain-state",
            "The request conflicts with current domain state.");

    private final HttpStatus status;
    private final String code;
    private final String title;
    private final String type;
    private final String defaultDetail;

    CoreProblemCode(HttpStatus status, String code, String title, String type, String defaultDetail) {
        this.status = status;
        this.code = code;
        this.title = title;
        this.type = type;
        this.defaultDetail = defaultDetail;
    }

    @Override
    public HttpStatus status() {
        return status;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String title() {
        return title;
    }

    @Override
    public String type() {
        return type;
    }

    @Override
    public String defaultDetail() {
        return defaultDetail;
    }
}
