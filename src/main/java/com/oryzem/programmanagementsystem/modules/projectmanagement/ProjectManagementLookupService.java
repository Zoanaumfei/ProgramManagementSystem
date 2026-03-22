package com.oryzem.programmanagementsystem.modules.projectmanagement;

import com.oryzem.programmanagementsystem.platform.shared.ResourceNotFoundException;
import org.springframework.stereotype.Service;

@Service
class ProjectManagementLookupService {

    private final ProgramRepository programRepository;
    private final ProjectRepository projectRepository;
    private final ProductRepository productRepository;
    private final ItemRepository itemRepository;
    private final DeliverableRepository deliverableRepository;
    private final DeliverableDocumentRepository deliverableDocumentRepository;
    private final MilestoneTemplateRepository milestoneTemplateRepository;

    ProjectManagementLookupService(
            ProgramRepository programRepository,
            ProjectRepository projectRepository,
            ProductRepository productRepository,
            ItemRepository itemRepository,
            DeliverableRepository deliverableRepository,
            DeliverableDocumentRepository deliverableDocumentRepository,
            MilestoneTemplateRepository milestoneTemplateRepository) {
        this.programRepository = programRepository;
        this.projectRepository = projectRepository;
        this.productRepository = productRepository;
        this.itemRepository = itemRepository;
        this.deliverableRepository = deliverableRepository;
        this.deliverableDocumentRepository = deliverableDocumentRepository;
        this.milestoneTemplateRepository = milestoneTemplateRepository;
    }

    ProgramEntity findProgram(String programId) {
        return programRepository.findById(programId)
                .orElseThrow(() -> new ResourceNotFoundException("Program", programId));
    }

    ProjectEntity findProject(String projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
    }

    ProductEntity findProduct(String productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
    }

    ItemEntity findItem(String itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item", itemId));
    }

    DeliverableEntity findDeliverable(String deliverableId) {
        return deliverableRepository.findById(deliverableId)
                .orElseThrow(() -> new ResourceNotFoundException("Deliverable", deliverableId));
    }

    DeliverableDocumentEntity findDeliverableDocument(String deliverableId, String documentId) {
        DeliverableDocumentEntity document = deliverableDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Deliverable document", documentId));
        if (!document.getDeliverable().getId().equals(deliverableId)) {
            throw new IllegalArgumentException("Document does not belong to the informed deliverable.");
        }
        return document;
    }

    MilestoneTemplateEntity findMilestoneTemplate(String milestoneTemplateId) {
        return milestoneTemplateRepository.findById(milestoneTemplateId)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone template", milestoneTemplateId));
    }
}
