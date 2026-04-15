package com.oryzem.programmanagementsystem.modules.projectmanagement.api;

import com.oryzem.programmanagementsystem.modules.projectmanagement.application.query.GetProjectDashboardQuery;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/dashboard")
public class ProjectDashboardController {

    private final ProjectApiSupport apiSupport;
    private final GetProjectDashboardQuery getProjectDashboardQuery;

    public ProjectDashboardController(ProjectApiSupport apiSupport, GetProjectDashboardQuery getProjectDashboardQuery) {
        this.apiSupport = apiSupport;
        this.getProjectDashboardQuery = getProjectDashboardQuery;
    }

    @GetMapping
    public ProjectDashboardDtos.ProjectDashboardResponse getDashboard(Authentication authentication, @PathVariable String projectId, @RequestParam(required = false) String structureNodeId) {
        return ProjectDashboardDtos.ProjectDashboardResponse.from(getProjectDashboardQuery.execute(projectId, structureNodeId, apiSupport.actor(authentication)));
    }
}

