package com.oryzem.programmanagementsystem.modules.projectmanagement.api;

import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUserMapper;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
class ProjectApiSupport {

    private final AuthenticatedUserMapper authenticatedUserMapper;

    ProjectApiSupport(AuthenticatedUserMapper authenticatedUserMapper) {
        this.authenticatedUserMapper = authenticatedUserMapper;
    }

    AuthenticatedUser actor(Authentication authentication) {
        return authenticatedUserMapper.from(authentication);
    }
}
