package com.oryzem.programmanagementsystem.modules.projectmanagement;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PortfolioResetService implements PortfolioResetPort {

    private final ProgramRepository programRepository;
    private final MilestoneTemplateRepository milestoneTemplateRepository;
    private final PortfolioResetTenantPort portfolioResetTenantPort;

    public PortfolioResetService(
            ProgramRepository programRepository,
            MilestoneTemplateRepository milestoneTemplateRepository,
            PortfolioResetTenantPort portfolioResetTenantPort) {
        this.programRepository = programRepository;
        this.milestoneTemplateRepository = milestoneTemplateRepository;
        this.portfolioResetTenantPort = portfolioResetTenantPort;
    }

    @Override
    @Transactional
    public void clearAll() {
        programRepository.deleteAll();
        milestoneTemplateRepository.deleteAll();
        portfolioResetTenantPort.clearOrganizations();
    }
}
