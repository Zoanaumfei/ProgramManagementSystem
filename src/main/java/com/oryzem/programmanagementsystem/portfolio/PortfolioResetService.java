package com.oryzem.programmanagementsystem.portfolio;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PortfolioResetService {

    private final ProgramRepository programRepository;
    private final MilestoneTemplateRepository milestoneTemplateRepository;
    private final OrganizationRepository organizationRepository;

    public PortfolioResetService(
            ProgramRepository programRepository,
            MilestoneTemplateRepository milestoneTemplateRepository,
            OrganizationRepository organizationRepository) {
        this.programRepository = programRepository;
        this.milestoneTemplateRepository = milestoneTemplateRepository;
        this.organizationRepository = organizationRepository;
    }

    @Transactional
    public void clearAll() {
        programRepository.deleteAll();
        milestoneTemplateRepository.deleteAll();
        organizationRepository.deleteAll();
    }
}
