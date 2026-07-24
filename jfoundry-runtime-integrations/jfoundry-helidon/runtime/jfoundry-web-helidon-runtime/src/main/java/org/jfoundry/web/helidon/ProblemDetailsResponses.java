package org.jfoundry.web.helidon;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.jfoundry.problem.ProblemCatalog;
import org.jfoundry.problem.ProblemDescriptor;

final class ProblemDetailsResponses {

    private static final String PROBLEM_JSON = "application/problem+json";

    private ProblemDetailsResponses() {
    }

    static Response forException(Exception exception) {
        return problem(ProblemCatalog.forException(exception), null);
    }

    static Response forHttpStatus(int status, MultivaluedMap<String, Object> headers) {
        return problem(ProblemCatalog.forHttpStatus(status), headers);
    }

    private static Response problem(ProblemDescriptor descriptor, MultivaluedMap<String, Object> headers) {
        Response.ResponseBuilder response = Response.status(descriptor.status())
                .type(PROBLEM_JSON)
                .entity(problemJson(descriptor));
        if (headers != null) {
            headers.forEach((name, values) -> {
                if (!HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(name) && !HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(name)) {
                    values.forEach(value -> response.header(name, value));
                }
            });
        }
        return response.build();
    }

    private static JsonObject problemJson(ProblemDescriptor descriptor) {
        return Json.createObjectBuilder()
                .add("type", descriptor.type().toString())
                .add("title", descriptor.title())
                .add("status", descriptor.status())
                .add("detail", descriptor.detail())
                .add("code", descriptor.code())
                .build();
    }
}
