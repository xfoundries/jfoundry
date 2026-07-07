package org.jfoundry.webmvc.spring;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HttpProblemCodeResolverTest {

    private final HttpProblemCodeResolver resolver = new HttpProblemCodeResolver();

    @Test
    void resolvesSpringMvcExceptionsToHttpProblemCodes() {
        assertThat(resolver.resolve(new HttpRequestMethodNotSupportedException("POST", List.of("GET")),
                HttpStatus.METHOD_NOT_ALLOWED)).isEqualTo(HttpProblemCode.METHOD_NOT_ALLOWED);
        assertThat(resolver.resolve(new HttpMediaTypeNotSupportedException(MediaType.APPLICATION_XML,
                List.of(MediaType.APPLICATION_JSON)), HttpStatus.UNSUPPORTED_MEDIA_TYPE))
                .isEqualTo(HttpProblemCode.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    void fallsBackToStatusCodeWhenExceptionIsGeneric() {
        assertThat(resolver.resolve(new RuntimeException("missing"), HttpStatus.NOT_FOUND))
                .isEqualTo(HttpProblemCode.NOT_FOUND);
    }
}
