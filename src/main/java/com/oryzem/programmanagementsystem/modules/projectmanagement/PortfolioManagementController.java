package com.oryzem.programmanagementsystem.modules.projectmanagement;

import com.oryzem.programmanagementsystem.platform.shared.FeatureTemporarilyUnavailableException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portfolio")
public class PortfolioManagementController {

    private static final String MESSAGE =
            "Portfolio is temporarily unavailable while Oryzem focuses on the User + Organization core. "
                    + "Use /api/access/organizations for organization management.";

    @RequestMapping(
            value = {"", "/{*path}"},
            method = {
                    org.springframework.web.bind.annotation.RequestMethod.GET,
                    org.springframework.web.bind.annotation.RequestMethod.POST,
                    org.springframework.web.bind.annotation.RequestMethod.PUT,
                    org.springframework.web.bind.annotation.RequestMethod.DELETE,
                    org.springframework.web.bind.annotation.RequestMethod.PATCH
            })
    public void portfolioTemporarilyUnavailable() {
        throw new FeatureTemporarilyUnavailableException(MESSAGE);
    }
}

