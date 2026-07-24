package org.jfoundry.web.helidon;

import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import jakarta.json.JsonObject;
import org.jfoundry.application.exception.InvalidArgumentException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemDetailsExceptionMapperTest {

    @Test
    void rendersJfoundryExceptionsAsProblemJson() {
        Response response = new ProblemDetailsExceptionMappers.InvalidArgumentMapper()
                .toResponse(new InvalidArgumentException("order id is required"));

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getMediaType().toString()).isEqualTo("application/problem+json");
        assertThat(response.getEntity()).isInstanceOf(JsonObject.class);
        JsonObject problem = (JsonObject) response.getEntity();
        assertThat(problem.getString("type")).isEqualTo("urn:jfoundry:problem:invalid-argument");
        assertThat(problem.getString("title")).isEqualTo("Invalid argument");
        assertThat(problem.getInt("status")).isEqualTo(400);
        assertThat(problem.getString("detail")).isEqualTo("order id is required");
        assertThat(problem.getString("code")).isEqualTo("INVALID_ARGUMENT");
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

    @Test
    void exposesEachMapperAsAJaxRsProvider() {
        assertThat(ProblemDetailsExceptionMappers.InvalidArgumentMapper.class.isAnnotationPresent(Provider.class)).isTrue();
        assertThat(ProblemDetailsExceptionMappers.NotFoundMapper.class.isAnnotationPresent(Provider.class)).isTrue();
        assertThat(ProblemDetailsExceptionMappers.ConflictMapper.class.isAnnotationPresent(Provider.class)).isTrue();
        assertThat(ProblemDetailsExceptionMappers.ExternalAccessMapper.class.isAnnotationPresent(Provider.class)).isTrue();
        assertThat(ProblemDetailsExceptionMappers.DomainRuleViolationMapper.class.isAnnotationPresent(Provider.class)).isTrue();
        assertThat(ProblemDetailsExceptionMappers.DomainStateMapper.class.isAnnotationPresent(Provider.class)).isTrue();
        assertThat(ProblemDetailsExceptionMappers.WebApplicationMapper.class.isAnnotationPresent(Provider.class)).isTrue();
    }
}
