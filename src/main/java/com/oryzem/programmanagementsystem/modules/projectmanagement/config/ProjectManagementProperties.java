package com.oryzem.programmanagementsystem.modules.projectmanagement.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "app.project-management")
public class ProjectManagementProperties {

    private boolean enabled = true;
    private boolean documentsEnabled = true;

    @Min(1)
    private int pendingReviewPageSize = 25;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isDocumentsEnabled() {
        return documentsEnabled;
    }

    public void setDocumentsEnabled(boolean documentsEnabled) {
        this.documentsEnabled = documentsEnabled;
    }

    public int getPendingReviewPageSize() {
        return pendingReviewPageSize;
    }

    public void setPendingReviewPageSize(int pendingReviewPageSize) {
        this.pendingReviewPageSize = pendingReviewPageSize;
    }
}
