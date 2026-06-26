/**
 * Application-layer messaging SPI.
 *
 * <p>This package defines framework-neutral outbound messaging contracts such as
 * {@code MessageSender} and {@code PayloadSerializer}. It belongs to the application core
 * because it expresses how jfoundry application services hand off messages to external systems
 * without committing to Spring, Kafka, Jackson, or any specific broker/runtime adapter.
 *
 * <p>Although the concrete adapters live elsewhere, this package is intentionally adjacent to the
 * domain-event externalization flow: domain events are translated into broker-neutral payloads in
 * the application layer, and infrastructure adapters are responsible only for actual delivery or
 * serialization mechanics.
 */
@org.jmolecules.architecture.onion.simplified.ApplicationRing
package org.jfoundry.application.messaging;
