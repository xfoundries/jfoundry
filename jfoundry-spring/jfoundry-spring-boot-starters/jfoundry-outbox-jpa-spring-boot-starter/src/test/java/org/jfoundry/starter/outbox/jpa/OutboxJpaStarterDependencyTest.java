package org.jfoundry.starter.outbox.jpa;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxJpaStarterDependencyTest {

    private static final String MAVEN_POM_NAMESPACE = "http://maven.apache.org/POM/4.0.0";

    @Test
    void declaresTheExplicitJPAOutboxCapabilityDependencies() throws Exception {
        assertThat(dependencyArtifactIds(Path.of("pom.xml")))
                .contains("jfoundry-outbox-spring-boot-starter", "jfoundry-outbox-jpa", "spring-boot-starter-data-jpa");
    }

    @Test
    void businessJpaStarterDoesNotIncludeReliableMessagingStores() throws Exception {
        assertThat(dependencyArtifactIds(Path.of("..", "jfoundry-jpa-spring-boot-starter", "pom.xml")))
                .doesNotContain("jfoundry-outbox-jpa", "jfoundry-inbox-jpa");
    }

    private Set<String> dependencyArtifactIds(Path pom) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document document = factory.newDocumentBuilder().parse(pom.toFile());
        NodeList dependencies = document.getElementsByTagNameNS(MAVEN_POM_NAMESPACE, "dependency");
        Set<String> artifactIds = new LinkedHashSet<>();
        for (int index = 0; index < dependencies.getLength(); index++) {
            Element dependency = (Element) dependencies.item(index);
            artifactIds.add(dependency.getElementsByTagNameNS(MAVEN_POM_NAMESPACE, "artifactId")
                    .item(0)
                    .getTextContent());
        }
        return artifactIds;
    }
}
