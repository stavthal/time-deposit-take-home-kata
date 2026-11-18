package org.ikigaidigital;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/** Guards the hexagonal rules: the core never depends on Spring, JPA or adapters. */
@AnalyzeClasses(packages = "org.ikigaidigital", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule coreIsFrameworkFree = noClasses()
            .that().resideInAnyPackage("..domain..", "..interest..", "..application..")
            .or().belongToAnyOf(TimeDeposit.class, TimeDepositCalculator.class)
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta.persistence..");

    @ArchTest
    static final ArchRule coreDoesNotKnowAdapters = noClasses()
            .that().resideInAnyPackage("..domain..", "..interest..", "..application..")
            .should().dependOnClassesThat().resideInAnyPackage("..adapter..", "..config..");
}
