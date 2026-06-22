/**
 * Test helper module — infrastructure-layer support for business-side tests.
 *
 * <p>Annotated with {@link org.jfoundry.architecture.layered.InfrastructureLayer} because this
 * package contains concrete Repository implementations ({@link org.jfoundry.test.InMemoryRepository})
 * and test fixtures that play the infrastructure role in business-side test suites. This also
 * serves as a dogfooding example: the framework's own rules must pass against the framework's
 * own code.
 */
@org.jfoundry.architecture.layered.InfrastructureLayer
package org.jfoundry.test;
