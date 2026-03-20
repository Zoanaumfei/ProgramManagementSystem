package com.oryzem.programmanagementsystem.portfolio;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface OrganizationRepository extends JpaRepository<OrganizationEntity, String> {

    List<OrganizationEntity> findAllByOrderByNameAsc();

    List<OrganizationEntity> findAllByCustomerOrganizationIdOrderByNameAsc(String customerOrganizationId);

    List<OrganizationEntity> findAllByParentOrganizationIdOrderByNameAsc(String parentOrganizationId);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, String id);
}

interface ProgramRepository extends JpaRepository<ProgramEntity, String> {

    List<ProgramEntity> findAllByOrderByCreatedAtAsc();

    boolean existsByCodeIgnoreCase(String code);
}

interface ProjectRepository extends JpaRepository<ProjectEntity, String> {

    boolean existsByCodeIgnoreCase(String code);
}

interface ProductRepository extends JpaRepository<ProductEntity, String> {

    boolean existsByCodeIgnoreCase(String code);
}

interface ItemRepository extends JpaRepository<ItemEntity, String> {

    boolean existsByCodeIgnoreCase(String code);
}

interface DeliverableRepository extends JpaRepository<DeliverableEntity, String> {
}

interface DeliverableDocumentRepository extends JpaRepository<DeliverableDocumentEntity, String> {

    List<DeliverableDocumentEntity> findByDeliverableIdOrderByCreatedAtAsc(String deliverableId);
}

interface MilestoneTemplateRepository extends JpaRepository<MilestoneTemplateEntity, String> {

    List<MilestoneTemplateEntity> findAllByOrderByNameAsc();
}
