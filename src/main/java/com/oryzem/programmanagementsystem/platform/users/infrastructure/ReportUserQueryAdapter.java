package com.oryzem.programmanagementsystem.platform.users.infrastructure;

import com.oryzem.programmanagementsystem.modules.reports.ReportUserQueryPort;
import com.oryzem.programmanagementsystem.platform.users.domain.ManagedUser;
import com.oryzem.programmanagementsystem.platform.users.domain.UserRepository;
import org.springframework.stereotype.Component;

@Component
class ReportUserQueryAdapter implements ReportUserQueryPort {

    private final UserRepository userRepository;

    ReportUserQueryAdapter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public java.util.List<ReportUserView> findUsers(String tenantId) {
        java.util.List<ManagedUser> users = tenantId == null
                ? userRepository.findAll()
                : userRepository.findByTenantId(tenantId);
        return users.stream()
                .map(user -> new ReportUserView(
                        user.role(),
                        user.status().name(),
                        user.tenantType()))
                .toList();
    }
}
