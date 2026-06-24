package org.jfoundry.application.messaging.externalization;

/// Broker-neutral metadata used for aggregate-scoped routing and ordering.
public record AggregateRoutingMetadata(String aggregateType, String aggregateId, Long aggregateVersion) {
}
