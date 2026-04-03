package com.oryzem.programmanagementsystem.app.api;

import com.oryzem.programmanagementsystem.platform.access.OperationalOverviewService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/operational")
public class OperationalOverviewController {

    private final OperationalOverviewService operationalOverviewService;

    public OperationalOverviewController(OperationalOverviewService operationalOverviewService) {
        this.operationalOverviewService = operationalOverviewService;
    }

    @GetMapping("/overview")
    public OperationalOverviewResponse overview(
            @RequestParam(name = "tenantId", required = false) List<String> tenantIds,
            @RequestParam(name = "tenantTier", required = false) String tenantTier,
            @RequestParam(name = "path", required = false) String path,
            @RequestParam(name = "activeOnly", required = false) Boolean activeOnly,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to) {
        return operationalOverviewService.getOverview(tenantIds, tenantTier, path, activeOnly, from, to);
    }
}
