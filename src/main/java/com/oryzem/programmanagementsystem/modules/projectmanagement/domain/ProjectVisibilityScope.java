package com.oryzem.programmanagementsystem.modules.projectmanagement.domain;

public enum ProjectVisibilityScope {
    /**
     * Hidden from external project actors. Only platform-internal privileged actors bypass this.
     */
    INTERNAL_ONLY,
    /**
     * Visible to any active project participant organization or assigned project member.
     */
    ALL_PROJECT_PARTICIPANTS,
    /**
     * Visible to lead organization, project managers/coordinators, and the assigned responsible/approver.
     */
    RESPONSIBLE_AND_APPROVER,
    /**
     * Visible to lead organization plus project managers/coordinators.
     */
    LEAD_ONLY
}
