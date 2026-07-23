package org.jfoundry.test.archunit;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;

@AnalyzeClasses(packages = "org.jfoundry.test.archunit.fixture.hexagonal")
class HexagonalRulesDiscoveryTest {

    @ArchTest
    static final ArchTests architecture = JFoundryRules.hexagonalStrict();
}
