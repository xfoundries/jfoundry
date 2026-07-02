package PACKAGE_NAME;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.jfoundry.test.archunit.JFoundryRules;

@AnalyzeClasses(packages = "PACKAGE_NAME")
class OnionSimpleArchitectureTest {

    @ArchTest
    ArchRule[] jfoundryRules = JFoundryRules.onionSimple();

    @ArchTest
    ArchRule[] jmoleculesDddRules = JFoundryRules.jmoleculesDdd();

    @ArchTest
    ArchRule[] aggregateRepositoryRules = JFoundryRules.aggregateRepositoryConventions();
}

