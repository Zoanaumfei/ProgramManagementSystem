package com.oryzem.programmanagementsystem.modules.reports;

import com.oryzem.programmanagementsystem.platform.authorization.Role;
import com.oryzem.programmanagementsystem.platform.authorization.TenantType;
import java.util.List;

public interface ReportUserQueryPort {

    List<ReportUserView> findUsers(String tenantId);

    record ReportUserView(
            Role role,
            String status,
            TenantType tenantType) {
    }
}
