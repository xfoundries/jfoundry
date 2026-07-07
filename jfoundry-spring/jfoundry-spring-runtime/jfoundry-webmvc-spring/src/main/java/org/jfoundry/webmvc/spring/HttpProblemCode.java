package org.jfoundry.webmvc.spring;

import org.springframework.http.HttpStatus;

/**
 * ProblemDetail codes for HTTP and Spring MVC request-processing errors.
 */
public enum HttpProblemCode implements ProblemCode {

    BAD_REQUEST(HttpStatus.BAD_REQUEST, "HTTP_BAD_REQUEST", "Bad request", "http-bad-request",
            "The request is invalid."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "HTTP_METHOD_NOT_ALLOWED", "Method not allowed",
            "http-method-not-allowed", "The HTTP method is not allowed for this resource."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "HTTP_UNSUPPORTED_MEDIA_TYPE",
            "Unsupported media type", "http-unsupported-media-type", "The request media type is not supported."),
    NOT_ACCEPTABLE(HttpStatus.NOT_ACCEPTABLE, "HTTP_NOT_ACCEPTABLE", "Not acceptable", "http-not-acceptable",
            "The requested representation is not available."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "HTTP_NOT_FOUND", "Not found", "http-not-found",
            "The requested resource was not found."),
    PAYLOAD_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "HTTP_PAYLOAD_TOO_LARGE", "Payload too large",
            "http-payload-too-large", "The request payload is too large."),
    REQUEST_TIMEOUT(HttpStatus.SERVICE_UNAVAILABLE, "HTTP_REQUEST_TIMEOUT", "Request timeout",
            "http-request-timeout", "The request timed out."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "HTTP_INTERNAL_ERROR", "Internal server error",
            "http-internal-error", "The server failed to process the request.");

    private final HttpStatus status;
    private final String code;
    private final String title;
    private final String type;
    private final String defaultDetail;

    HttpProblemCode(HttpStatus status, String code, String title, String type, String defaultDetail) {
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
