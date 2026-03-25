package com.oryzem.programmanagementsystem.platform.users.infrastructure;

import com.oryzem.programmanagementsystem.modules.reports.ReportUserQueryPort;
import com.oryzem.programmanagementsystem.platform.access.AccessContextService;
import com.oryzem.programmanagementsystem.platform.users.domain.ManagedUser;
import com.oryzem.programmanagementsystem.platform.users.domain.UserRepository;
import org.springframework.stereotype.Component;

@Component
class ReportUserQueryAdapter implements ReportUserQueryPort {

    private final UserRepository userRepository;
    private final AccessContextService accessContextService;

    ReportUserQueryAdapter(UserRepository userRepository, AccessContextService accessContextService) {
        this.userRepository = userRepository;
        this.accessContextService = accessContextService;
    }

    @Override
    public java.util.List<ReportUserView> findUsers(String tenantId) {
        java.util.List<ManagedUser> users = tenantId == null
                ? userRepository.findAll()
                : userRepository.findByTenantId(tenantId);
        return users.stream()
                .map(user -> {
                    var context = accessContextService.requireActiveContext(user);
                    return new ReportUserView(
                            accessContextService.resolvePrimaryRole(user).orElse(null),
                            user.status().name(),
                            context.tenantType());
                })
                .toList();
    }
}
