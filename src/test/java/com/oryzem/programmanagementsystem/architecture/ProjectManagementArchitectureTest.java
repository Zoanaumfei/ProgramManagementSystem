package com.oryzem.programmanagementsystem.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.oryzem.programmanagementsystem.modules.documentmanagement.application.DocumentAdministrationFacade;
import com.oryzem.programmanagementsystem.modules.documentmanagement.application.DocumentContextRef;
import com.oryzem.programmanagementsystem.modules.documentmanagement.application.DocumentPurgeSummary;
import com.oryzem.programmanagementsystem.modules.documentmanagement.application.DocumentPublicFacade;
import com.oryzem.programmanagementsystem.modules.documentmanagement.application.DocumentView;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentContextPolicy;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentContextPolicyProvider;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentContextType;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentStatus;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
        packages = "com.oryzem.programmanagementsystem",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ProjectManagementArchitectureTest {

    @ArchTest
    static final ArchRule project_domain_should_not_depend_on_infrastructure =
            classes()
                    .that().resideInAPackage("..modules.projectmanagement.domain..")
                    .should().onlyDependOnClassesThat().resideInAnyPackage(
                            "java..",
                            "..modules.projectmanagement.domain..",
                            "..platform.shared..")
                    .because("project-management domain should stay framework-free and isolated from infrastructure");

    @ArchTest
    static final ArchRule project_domain_should_not_depend_on_spring_or_jpa =
            noClasses()
                    .that().resideInAPackage("..modules.projectmanagement.domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework..",
                            "jakarta.persistence..")
                    .because("domain types should not know framework or persistence annotations");

    @ArchTest
    static final ArchRule project_api_should_not_touch_repositories_directly =
            noClasses()
                    .that().resideInAPackage("..modules.projectmanagement.api..")
                    .should().dependOnClassesThat().resideInAPackage("..modules.projectmanagement.infrastructure..");

    @ArchTest
    static final ArchRule project_application_ports_should_not_depend_on_infrastructure =
            noClasses()
                    .that().resideInAPackage("..modules.projectmanagement.application.port..")
                    .should().dependOnClassesThat().resideInAPackage("..modules.projectmanagement.infrastructure..")
                    .because("application ports must define clean contracts without leaking persistence details");

    @ArchTest
    static final ArchRule project_application_should_not_depend_on_persistence_entities =
            noClasses()
                    .that().resideInAPackage("..modules.projectmanagement.application..")
                    .should().dependOnClassesThat().haveSimpleNameEndingWith("Entity")
                    .because("application should depend on ports, domain models, and read models rather than persistence entities");

    @ArchTest
    static final ArchRule project_application_should_not_depend_on_spring_data_repositories =
            noClasses()
                    .that().resideInAPackage("..modules.projectmanagement.application..")
                    .should().dependOnClassesThat().haveSimpleNameStartingWith("SpringData")
                    .because("application must reach persistence through ports rather than Spring Data repositories");

    @ArchTest
    static final ArchRule project_policies_should_not_depend_on_persistence_entities =
            noClasses()
                    .that().resideInAPackage("..modules.projectmanagement.application..")
                    .and().haveSimpleNameEndingWith("Policy")
                    .should().dependOnClassesThat().haveSimpleNameEndingWith("Entity")
                    .because("authorization and visibility rules should depend on snapshots, not persistence models");

    @ArchTest
    static final ArchRule project_controllers_should_not_depend_on_repositories_directly =
            noClasses()
                    .that().resideInAPackage("..modules.projectmanagement.api..")
                    .should().dependOnClassesThat().haveSimpleNameContaining("Repository");

    @ArchTest
    static final ArchRule document_management_should_not_depend_on_project_management =
            noClasses()
                    .that().resideInAPackage("..modules.documentmanagement..")
                    .should().dependOnClassesThat().resideInAPackage("..modules.projectmanagement..");

    @ArchTest
    static final ArchRule project_infrastructure_should_stay_internal_to_project_management =
            noClasses()
                    .that().resideOutsideOfPackage("..modules.projectmanagement..")
                    .should().dependOnClassesThat().resideInAPackage("..modules.projectmanagement.infrastructure..");

    @ArchTest
    static final ArchRule project_entities_should_stay_internal_to_project_management =
            noClasses()
                    .that().resideOutsideOfPackage("..modules.projectmanagement..")
                    .should().dependOnClassesThat(projectManagementEntityClasses())
                    .because("project persistence entities are internal implementation details");

    @ArchTest
    static final ArchRule project_module_should_not_expose_internal_packages_to_other_modules =
            noClasses()
                    .that().resideOutsideOfPackage("..modules.projectmanagement..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..modules.projectmanagement.api..",
                            "..modules.projectmanagement.application..",
                            "..modules.projectmanagement.domain..",
                            "..modules.projectmanagement.config..",
                            "..modules.projectmanagement.support..");

    @ArchTest
    static final ArchRule document_management_integrations_should_use_only_published_contract =
            noClasses()
                    .that().resideOutsideOfPackage("..modules.documentmanagement..")
                    .should().dependOnClassesThat(documentManagementInternalClasses())
                    .because("cross-module document integrations must go through the published facade contract");

    private static DescribedPredicate<JavaClass> documentManagementInternalClasses() {
        return new DescribedPredicate<>("document-management internal classes outside the published contract") {
            @Override
            public boolean test(JavaClass javaClass) {
                String packageName = javaClass.getPackageName();
                String className = javaClass.getName();
                return packageName.contains(".modules.documentmanagement.")
                        && !className.equals(DocumentAdministrationFacade.class.getName())
                        && !className.equals(DocumentContextRef.class.getName())
                        && !className.equals(DocumentPurgeSummary.class.getName())
                        && !className.equals(DocumentPublicFacade.class.getName())
                        && !className.equals(DocumentView.class.getName())
                        && !className.equals(DocumentStatus.class.getName())
                        && !className.equals(DocumentContextType.class.getName())
                        && !className.equals(DocumentContextPolicy.class.getName())
                        && !className.equals(DocumentContextPolicyProvider.class.getName());
            }
        };
    }

    private static DescribedPredicate<JavaClass> projectManagementEntityClasses() {
        return new DescribedPredicate<>("project-management persistence entities") {
            @Override
            public boolean test(JavaClass javaClass) {
                return javaClass.getPackageName().contains(".modules.projectmanagement.infrastructure")
                        && javaClass.getSimpleName().endsWith("Entity");
            }
        };
    }
}
