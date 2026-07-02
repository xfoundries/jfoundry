package PACKAGE_NAME;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.jfoundry.test.archunit.JFoundryRules;

@AnalyzeClasses(packages = "PACKAGE_NAME")
class HexagonalArchitectureTest {

    @ArchTest
    ArchRule[] jfoundryRules = JFoundryRules.hexagonalStrict();

    @ArchTest
    ArchRule[] jmoleculesDddRules = JFoundryRules.jmoleculesDdd();

    @ArchTest
    ArchRule[] aggregateRepositoryRules = JFoundryRules.aggregateRepositoryConventions();
}

