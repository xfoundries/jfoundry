package org.jfoundry.autoconfigure;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class AutoConfigurationPackageArchitectureTest {

    @Test
    void autoconfigureRootPackageIsNotAHexagonalAdapter() throws ClassNotFoundException {
        Class<?> packageInfo = Class.forName("org.jfoundry.autoconfigure.package-info");

        assertThat(Arrays.stream(packageInfo.getAnnotations())
                .map(annotation -> annotation.annotationType().getName()))
                .doesNotContain(
                        "org.jfoundry.architecture.hexagonal.Adapter",
                        "org.jfoundry.architecture.hexagonal.PrimaryAdapter",
                        "org.jfoundry.architecture.hexagonal.SecondaryAdapter",
                        "org.jmolecules.architecture.hexagonal.Adapter",
                        "org.jmolecules.architecture.hexagonal.PrimaryAdapter",
                        "org.jmolecules.architecture.hexagonal.SecondaryAdapter",
                        "org.jfoundry.architecture.onion.simplified.DomainRing",
                        "org.jfoundry.architecture.onion.simplified.ApplicationRing",
                        "org.jfoundry.architecture.onion.simplified.InfrastructureRing",
                        "org.jmolecules.architecture.onion.simplified.DomainRing",
                        "org.jmolecules.architecture.onion.simplified.ApplicationRing",
                        "org.jmolecules.architecture.onion.simplified.InfrastructureRing");
    }
}
