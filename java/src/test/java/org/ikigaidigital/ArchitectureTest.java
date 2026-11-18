package org.ikigaidigital;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/** Guards the hexagonal rules: the core never depends on Spring, JPA or adapters. */
@AnalyzeClasses(packages = "org.ikigaidigital", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    // "org.ikigaidigital" alone is the root package only: TimeDeposit and TimeDepositCalculator.
    private static final String[] CORE = {"org.ikigaidigital", "..domain..", "..interest..", "..application.."};

    @ArchTest
    static final ArchRule coreIsFrameworkFree = noClasses()
            .that().resideInAnyPackage(CORE)
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta.persistence..");

    @ArchTest
    static final ArchRule coreDoesNotKnowAdapters = noClasses()
            .that().resideInAnyPackage(CORE)
            .should().dependOnClassesThat().resideInAnyPackage("..adapter..", "..config..");
}
