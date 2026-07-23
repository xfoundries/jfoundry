package org.jfoundry.problem;

import java.net.URI;

/// Runtime-neutral representation of an RFC 9457 problem response.
public record ProblemDescriptor(int status, String code, String title, URI type, String detail) {
}
