package com.code.atlas.web.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

@AnalyzeClasses(
        packages = "com.code.atlas.web",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class ArchitectureRulesTest {

    @ArchTest
    static final ArchRule restControllersShouldResideInControllerPackage =
            classes().that().areAnnotatedWith(RestController.class)
                    .and().resideOutsideOfPackage("..controller.page..")
                    .should().resideInAnyPackage("..controller..")
                    .because("RestControllers must live in com.code.atlas.web.controller");

    @ArchTest
    static final ArchRule pageControllersShouldResideInPagePackageAndBeNamedCorrectly =
            classes().that().areAnnotatedWith(Controller.class)
                    .and().areNotMetaAnnotatedWith(ControllerAdvice.class)
                    .should().resideInAPackage("..controller.page..")
                    .andShould().haveSimpleNameEndingWith("PageController")
                    .because("PageControllers must live in com.code.atlas.web.controller.page and end with PageController");

    @ArchTest
    static final ArchRule repositoriesShouldResideInRepositoryPackage =
            classes().that().areAnnotatedWith(Repository.class)
                    .or().implement(JpaRepository.class)
                    .should().resideInAnyPackage("..repository..")
                    .because("Repositories must live in com.code.atlas.web.repository");

    @ArchTest
    static final ArchRule entitiesShouldResideInDomainPackage =
            classes().that().areAnnotatedWith(Entity.class)
                    .should().resideInAnyPackage("..domain..")
                    .because("Entities must live in com.code.atlas.web.domain");

    @ArchTest
    static final ArchRule servicesShouldResideInServicePackage =
            classes().that().areAnnotatedWith(Service.class)
                    .should().resideInAnyPackage("..service..")
                    .because("Services must live in com.code.atlas.web.service");

    @ArchTest
    static final ArchRule serviceRecordsShouldResideInDtoPackage =
            classes().that().areRecords()
                    .and().resideInAnyPackage("..service..")
                    .and().resideOutsideOfPackage("..service.context..")
                    .should().resideInAnyPackage("..service.dto..")
                    .because("Service DTOs must live in com.code.atlas.web.service.dto");

    @ArchTest
    static final ArchRule servicesShouldNotBeInterfaces =
            classes().that().areAnnotatedWith(Service.class)
                    .should().notBeInterfaces()
                    .because("Service classes must not be interfaces");

    @ArchTest
    static final ArchRule repositoriesShouldBeInterfaces =
            classes().that().areAnnotatedWith(Repository.class)
                    .should().beInterfaces()
                    .because("Repository classes must be interfaces");

    @ArchTest
    static final ArchRule restControllersShouldNotAccessRepositories =
            noClasses().that().resideInAnyPackage("..controller..")
                    .and().areNotMetaAnnotatedWith(ControllerAdvice.class)
                    .should().accessClassesThat().resideInAnyPackage("..repository..")
                    .because("RestControllers must not access Repositories directly");

    @ArchTest
    static final ArchRule pageControllersShouldNotAccessRepositories =
            noClasses().that().resideInAPackage("..controller.page..")
                    .should().accessClassesThat().resideInAnyPackage("..repository..")
                    .because("PageControllers must not access Repositories directly");

    @ArchTest
    static final ArchRule servicesShouldNotAccessControllers =
            noClasses().that().resideInAnyPackage("..service..")
                    .should().accessClassesThat().resideInAnyPackage("..controller..")
                    .because("Services must not access Controllers");

    @ArchTest
    static final ArchRule fieldsShouldNotUseAutowired =
            noFields().should().beAnnotatedWith(Autowired.class)
                    .because("Field injection via @Autowired is prohibited; use constructor injection");

    @ArchTest
    static final ArchRule methodsShouldNotUseAutowired =
            noMethods().should().beAnnotatedWith(Autowired.class)
                    .because("Method injection via @Autowired is prohibited; use constructor injection");
}
