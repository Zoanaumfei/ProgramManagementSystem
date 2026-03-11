package com.oryzem.programmanagementsystem.authorization;

public enum AuthorizationRestriction {
    SAME_TENANT_ONLY,
    MANAGER_TARGET_ROLE_LIMIT,
    MEMBER_EDIT_FLOW_RESTRICTION,
    AUDIT_TRAIL_REQUIRED,
    JUSTIFICATION_REQUIRED,
    SENSITIVE_DATA_RESTRICTED,
    SUPPORT_SCOPE_RESTRICTION
}
