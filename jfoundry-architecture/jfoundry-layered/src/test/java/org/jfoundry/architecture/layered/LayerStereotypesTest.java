package org.jfoundry.architecture.layered;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;

import static org.assertj.core.api.Assertions.assertThat;

/// P3-2: all layered stereotypes must exist, be meta-annotated when a
/// jmolecules counterpart exists, and target package + type.
class LayerStereotypesTest {

    @Test
    void applicationLayerIsMetaAnnotated() {
        org.jmolecules.architecture.layered.ApplicationLayer meta =
                ApplicationLayer.class.getAnnotation(org.jmolecules.architecture.layered.ApplicationLayer.class);
        assertThat(meta).as("@ApplicationLayer must be meta-annotated with jmolecules @ApplicationLayer").isNotNull();
    }

    @Test
    void domainLayerIsMetaAnnotated() {
        org.jmolecules.architecture.layered.DomainLayer meta =
                DomainLayer.class.getAnnotation(org.jmolecules.architecture.layered.DomainLayer.class);
        assertThat(meta).as("@DomainLayer must be meta-annotated with jmolecules @DomainLayer").isNotNull();
    }

    @Test
    void interfaceLayerIsMetaAnnotated() {
        org.jmolecules.architecture.layered.InterfaceLayer meta =
                InterfaceLayer.class.getAnnotation(org.jmolecules.architecture.layered.InterfaceLayer.class);
        assertThat(meta).as("@InterfaceLayer must be meta-annotated with jmolecules @InterfaceLayer").isNotNull();
    }

    @Test
    void infrastructureLayerIsMetaAnnotated() {
        org.jmolecules.architecture.layered.InfrastructureLayer meta =
                InfrastructureLayer.class.getAnnotation(org.jmolecules.architecture.layered.InfrastructureLayer.class);
        assertThat(meta).as("@InfrastructureLayer must be meta-annotated with jmolecules @InfrastructureLayer").isNotNull();
    }

    @Test
    void allStereotypesTargetPackageAndType() {
        for (Class<? extends Annotation> stereotype : new Class[]{
                ApplicationLayer.class, DomainLayer.class,
                InterfaceLayer.class, InfrastructureLayer.class
        }) {
            java.lang.annotation.Target target = stereotype.getAnnotation(java.lang.annotation.Target.class);
            assertThat(target).as(stereotype.getSimpleName() + " must have @Target").isNotNull();
            assertThat(java.util.Arrays.asList(target.value()))
                    .as(stereotype.getSimpleName() + " must target PACKAGE and TYPE")
                    .contains(java.lang.annotation.ElementType.PACKAGE, java.lang.annotation.ElementType.TYPE);
        }
    }
}
