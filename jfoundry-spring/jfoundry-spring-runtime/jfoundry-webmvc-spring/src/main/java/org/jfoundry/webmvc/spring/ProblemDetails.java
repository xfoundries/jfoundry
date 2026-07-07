package org.jfoundry.webmvc.spring;

import org.springframework.http.ProblemDetail;

import java.net.URI;

/**
 * Factory methods for HTTP ProblemDetail responses.
 */
public final class ProblemDetails {

    public static final String PROBLEM_TYPE_URN_PREFIX = "urn:jfoundry:problem:";
    public static final String CODE_PROPERTY = "code";

    private ProblemDetails() {
    }

    public static ProblemDetail create(ProblemCode code) {
        return create(code, code.defaultDetail());
    }

    public static ProblemDetail create(ProblemCode code, String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(code.status(), detail);
        problemDetail.setTitle(code.title());
        problemDetail.setType(URI.create(PROBLEM_TYPE_URN_PREFIX + code.type()));
        problemDetail.setProperty(CODE_PROPERTY, code.code());
        return problemDetail;
    }
}
