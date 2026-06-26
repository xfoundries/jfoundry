/**
 * Spring adapter for application-layer domain event dispatch.
 *
 * <p>Types in this package implement the application-side {@code DomainEventDispatcher} contract
 * by orchestrating transactional outbox writing and Spring {@code ApplicationEventPublisher}
 * publication.
 */
@org.jmolecules.architecture.onion.simplified.InfrastructureRing
package org.jfoundry.infrastructure.messaging.spring.dispatcher;
