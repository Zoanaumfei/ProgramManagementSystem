package com.oryzem.programmanagementsystem.platform.users.domain;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String userId) {
        super("User not found: " + userId);
    }
}
