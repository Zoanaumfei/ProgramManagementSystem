package com.oryzem.programmanagementsystem.web;

import com.oryzem.programmanagementsystem.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.authorization.AuthenticatedUserMapper;
import com.oryzem.programmanagementsystem.reports.OperationsExportResponse;
import com.oryzem.programmanagementsystem.reports.OperationsReportResponse;
import com.oryzem.programmanagementsystem.reports.ReportManagementService;
import com.oryzem.programmanagementsystem.reports.ReportSummaryResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportManagementService reportManagementService;
    private final AuthenticatedUserMapper authenticatedUserMapper;

    public ReportController(
            ReportManagementService reportManagementService,
            AuthenticatedUserMapper authenticatedUserMapper) {
        this.reportManagementService = reportManagementService;
        this.authenticatedUserMapper = authenticatedUserMapper;
    }

    @GetMapping("/summary")
    public ReportSummaryResponse getSummary(
            Authentication authentication,
            @RequestParam(required = false) String tenantId,
            @RequestParam(defaultValue = "false") boolean supportOverride,
            @RequestParam(required = false) String justification) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return reportManagementService.getSummary(actor, tenantId, supportOverride, justification);
    }

    @GetMapping("/operations")
    public OperationsReportResponse getOperationsReport(
            Authentication authentication,
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "false") boolean supportOverride,
            @RequestParam(required = false) String justification) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return reportManagementService.getOperationsReport(actor, tenantId, status, supportOverride, justification);
    }

    @GetMapping("/operations/export")
    public OperationsExportResponse exportOperationsReport(
            Authentication authentication,
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "false") boolean includeSensitiveData,
            @RequestParam(defaultValue = "false") boolean maskedView,
            @RequestParam(defaultValue = "false") boolean supportOverride,
            @RequestParam(required = false) String justification) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return reportManagementService.exportOperationsReport(
                actor,
                tenantId,
                status,
                includeSensitiveData,
                maskedView,
                supportOverride,
                justification);
    }
}
