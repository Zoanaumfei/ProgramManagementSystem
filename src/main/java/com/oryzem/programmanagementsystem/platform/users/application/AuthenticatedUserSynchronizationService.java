package com.oryzem.programmanagementsystem.platform.users.application;

import com.oryzem.programmanagementsystem.platform.users.domain.ManagedUser;
import com.oryzem.programmanagementsystem.platform.users.domain.UserRepository;
import com.oryzem.programmanagementsystem.platform.users.domain.UserStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class AuthenticatedUserSynchronizationService {

    private final UserRepository userRepository;

    AuthenticatedUserSynchronizationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    void synchronizeAuthenticatedUser(String identitySubject, String identityUsername, String email) {
        boolean hasSubject = identitySubject != null && !identitySubject.isBlank();
        boolean hasUsername = identityUsername != null && !identityUsername.isBlank();
        boolean hasEmail = email != null && !email.isBlank();
        if (!hasSubject && !hasUsername && !hasEmail) {
            return;
        }

        ManagedUser user = null;
        if (hasSubject) {
            user = userRepository.findByIdentitySubject(identitySubject).orElse(null);
        }
        if (user == null && hasUsername) {
            user = userRepository.findByIdentityUsername(identityUsername).orElse(null);
        }
        if (user == null && hasEmail) {
            user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        }
        if (user == null) {
            return;
        }

        ManagedUser updated = user;
        if (hasSubject && (user.identitySubject() == null || !identitySubject.equals(user.identitySubject()))) {
            updated = updated.withIdentitySubject(identitySubject);
        }
        if (updated.status() == UserStatus.INVITED) {
            updated = updated.withStatus(UserStatus.ACTIVE);
        }

        if (!updated.equals(user)) {
            userRepository.save(updated);
        }
    }
}
