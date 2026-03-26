package com.oryzem.programmanagementsystem.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.oryzem.programmanagementsystem.app.bootstrap.BootstrapDataService;
import com.oryzem.programmanagementsystem.platform.audit.SpringDataAuditLogJpaRepository;
import com.oryzem.programmanagementsystem.platform.tenant.OrganizationDirectoryService;
import com.oryzem.programmanagementsystem.platform.users.infrastructure.JpaUserRepository;
import com.oryzem.programmanagementsystem.platform.users.infrastructure.SpringDataUserJpaRepository;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
        packages = "com.oryzem.programmanagementsystem",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureGuardrailsTest {

    @ArchTest
    static final ArchRule app_api_and_config_should_not_depend_on_business_modules =
            noClasses()
                    .that().resideInAnyPackage(
                            "..app.api..",
                            "..app.config..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..platform.users..",
                            "..platform.tenant..")
                    .because("app should stay focused on wiring and cross-cutting concerns");

    @ArchTest
    static final ArchRule bootstrap_should_not_depend_on_concrete_tenant_services =
            noClasses()
                    .that().haveFullyQualifiedName(BootstrapDataService.class.getName())
                    .should().dependOnClassesThat().haveFullyQualifiedName(OrganizationDirectoryService.class.getName())
                    .because("bootstrap should consume small module-facing ports when possible");

    @ArchTest
    static final ArchRule classes_outside_users_infrastructure_should_not_depend_on_users_infrastructure =
            noClasses()
                    .that().resideOutsideOfPackage("..platform.users.infrastructure..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..platform.users.infrastructure..")
                    .because("users infrastructure should stay encapsulated behind its domain and application contracts");

    @ArchTest
    static final ArchRule concrete_jpa_repositories_should_not_leak_across_modules =
            noClasses()
                    .that().resideOutsideOfPackages(
                            "..platform.users.infrastructure..",
                            "..platform.audit..",
                            "..platform.access..")
                    .should().dependOnClassesThat().haveFullyQualifiedName(JpaUserRepository.class.getName())
                    .orShould().dependOnClassesThat().haveFullyQualifiedName(SpringDataUserJpaRepository.class.getName())
                    .orShould().dependOnClassesThat().haveFullyQualifiedName(SpringDataAuditLogJpaRepository.class.getName())
                    .orShould().dependOnClassesThat().haveFullyQualifiedName("com.oryzem.programmanagementsystem.platform.access.SpringDataTenantJpaRepository")
                    .orShould().dependOnClassesThat().haveFullyQualifiedName("com.oryzem.programmanagementsystem.platform.access.SpringDataUserMembershipJpaRepository")
                    .orShould().dependOnClassesThat().haveFullyQualifiedName("com.oryzem.programmanagementsystem.platform.access.SpringDataMembershipRoleJpaRepository")
                    .because("cross-module code should use ports and domain contracts instead of concrete repositories");

    @ArchTest
    static final ArchRule generic_web_packages_should_not_host_controllers =
            noClasses()
                    .that().resideInAPackage("..web..")
                    .should().haveSimpleNameEndingWith("Controller")
                    .because("controllers should live in app/platform/modules packages, not in a generic web bucket");

    @ArchTest
    static final ArchRule shared_should_stay_minimal_and_neutral =
            classes()
                    .that().resideInAPackage("..platform.shared..")
                    .should().onlyDependOnClassesThat().resideInAnyPackage(
                            "java..",
                            "..platform.shared..")
                    .because("shared should not accumulate business or infrastructure knowledge");
}
