package org.jfoundry.web.helidon;

import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.jfoundry.application.exception.InvalidArgumentException;
import org.jfoundry.problem.ProblemDescriptor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemDetailsExceptionMapperTest {

    @Test
    void rendersJfoundryExceptionsAsProblemJson() {
        Response response = new ProblemDetailsExceptionMappers.InvalidArgumentMapper()
                .toResponse(new InvalidArgumentException("order id is required"));

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getMediaType().toString()).isEqualTo("application/problem+json");
        assertThat(response.getEntity()).isEqualTo(new ProblemDescriptor(400, "INVALID_ARGUMENT", "Invalid argument",
                java.net.URI.create("urn:jfoundry:problem:invalid-argument"), "order id is required"));
    }

    @Test
    void retainsAllowHeaderForMethodNotAllowedResponses() {
        Response source = Response.status(405).header(HttpHeaders.ALLOW, "GET, HEAD").build();
        Response response = new ProblemDetailsExceptionMappers.WebApplicationMapper()
                .toResponse(new NotAllowedException(source));

        assertThat(response.getStatus()).isEqualTo(405);
        assertThat(response.getHeaderString(HttpHeaders.ALLOW)).isEqualTo("GET, HEAD");
        assertThat(response.getMediaType().toString()).isEqualTo("application/problem+json");
    }
}
