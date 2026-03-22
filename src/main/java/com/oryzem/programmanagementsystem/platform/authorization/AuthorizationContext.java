package com.oryzem.programmanagementsystem.platform.authorization;

public final class AuthorizationContext {

    private final AppModule module;
    private final Action action;
    private final String resourceTenantId;
    private final TenantType resourceTenantType;
    private final String resourceId;
    private final String resourceOwnerUserId;
    private final String resourceStatus;
    private final Role targetRole;
    private final String targetUserId;
    private final boolean auditTrailEnabled;
    private final boolean supportOverride;
    private final boolean sensitiveDataRequested;
    private final boolean maskedViewRequested;
    private final String justification;

    private AuthorizationContext(Builder builder) {
        this.module = builder.module;
        this.action = builder.action;
        this.resourceTenantId = builder.resourceTenantId;
        this.resourceTenantType = builder.resourceTenantType;
        this.resourceId = builder.resourceId;
        this.resourceOwnerUserId = builder.resourceOwnerUserId;
        this.resourceStatus = builder.resourceStatus;
        this.targetRole = builder.targetRole;
        this.targetUserId = builder.targetUserId;
        this.auditTrailEnabled = builder.auditTrailEnabled;
        this.supportOverride = builder.supportOverride;
        this.sensitiveDataRequested = builder.sensitiveDataRequested;
        this.maskedViewRequested = builder.maskedViewRequested;
        this.justification = builder.justification;
    }

    public static Builder builder(AppModule module, Action action) {
        return new Builder(module, action);
    }

    public AppModule module() {
        return module;
    }

    public Action action() {
        return action;
    }

    public String resourceTenantId() {
        return resourceTenantId;
    }

    public TenantType resourceTenantType() {
        return resourceTenantType;
    }

    public String resourceId() {
        return resourceId;
    }

    public String resourceOwnerUserId() {
        return resourceOwnerUserId;
    }

    public String resourceStatus() {
        return resourceStatus;
    }

    public Role targetRole() {
        return targetRole;
    }

    public String targetUserId() {
        return targetUserId;
    }

    public boolean auditTrailEnabled() {
        return auditTrailEnabled;
    }

    public boolean supportOverride() {
        return supportOverride;
    }

    public boolean sensitiveDataRequested() {
        return sensitiveDataRequested;
    }

    public boolean maskedViewRequested() {
        return maskedViewRequested;
    }

    public String justification() {
        return justification;
    }

    public String effectiveResourceTenantId(AuthenticatedUser user) {
        return hasText(resourceTenantId) ? resourceTenantId : user.tenantId();
    }

    public TenantType effectiveResourceTenantType(AuthenticatedUser user) {
        return resourceTenantType != null ? resourceTenantType : user.tenantType();
    }

    public boolean isResourceOwnedBy(AuthenticatedUser user) {
        return hasText(resourceOwnerUserId) && resourceOwnerUserId.equals(user.subject());
    }

    public boolean hasJustification() {
        return hasText(justification);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public static final class Builder {
        private final AppModule module;
        private final Action action;
        private String resourceTenantId;
        private TenantType resourceTenantType;
        private String resourceId;
        private String resourceOwnerUserId;
        private String resourceStatus;
        private Role targetRole;
        private String targetUserId;
        private boolean auditTrailEnabled;
        private boolean supportOverride;
        private boolean sensitiveDataRequested;
        private boolean maskedViewRequested;
        private String justification;

        private Builder(AppModule module, Action action) {
            this.module = module;
            this.action = action;
        }

        public Builder resourceTenantId(String resourceTenantId) {
            this.resourceTenantId = resourceTenantId;
            return this;
        }

        public Builder resourceTenantType(TenantType resourceTenantType) {
            this.resourceTenantType = resourceTenantType;
            return this;
        }

        public Builder resourceId(String resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        public Builder resourceOwnerUserId(String resourceOwnerUserId) {
            this.resourceOwnerUserId = resourceOwnerUserId;
            return this;
        }

        public Builder resourceStatus(String resourceStatus) {
            this.resourceStatus = resourceStatus;
            return this;
        }

        public Builder targetRole(Role targetRole) {
            this.targetRole = targetRole;
            return this;
        }

        public Builder targetUserId(String targetUserId) {
            this.targetUserId = targetUserId;
            return this;
        }

        public Builder auditTrailEnabled(boolean auditTrailEnabled) {
            this.auditTrailEnabled = auditTrailEnabled;
            return this;
        }

        public Builder supportOverride(boolean supportOverride) {
            this.supportOverride = supportOverride;
            return this;
        }

        public Builder sensitiveDataRequested(boolean sensitiveDataRequested) {
            this.sensitiveDataRequested = sensitiveDataRequested;
            return this;
        }

        public Builder maskedViewRequested(boolean maskedViewRequested) {
            this.maskedViewRequested = maskedViewRequested;
            return this;
        }

        public Builder justification(String justification) {
            this.justification = justification;
            return this;
        }

        public AuthorizationContext build() {
            return new AuthorizationContext(this);
        }
    }
}
