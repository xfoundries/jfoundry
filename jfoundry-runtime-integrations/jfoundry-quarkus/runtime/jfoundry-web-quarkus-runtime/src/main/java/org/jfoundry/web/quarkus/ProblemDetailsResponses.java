package org.jfoundry.web.quarkus;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
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
                .entity(descriptor);
        if (headers != null) {
            headers.forEach((name, values) -> {
                if (!isEntityHeader(name)) {
                    values.forEach(value -> response.header(name, value));
                }
            });
        }
        return response.build();
    }

    private static boolean isEntityHeader(String name) {
        return HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(name) || HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(name);
    }
}
