package org.jfoundry.webmvc.spring;

import org.springframework.http.HttpStatus;

/**
 * Common contract for ProblemDetail codes exposed by the Spring MVC adapter.
 */
public interface ProblemCode {

    HttpStatus status();

    String code();

    String title();

    String type();

    String defaultDetail();
}
