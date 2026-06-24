package org.jfoundry.infrastructure.messaging.externalization;

/// Broker-neutral metadata used for aggregate-scoped routing and ordering.
public record AggregateRoutingMetadata(String aggregateType, String aggregateId, Long aggregateVersion) {
}
