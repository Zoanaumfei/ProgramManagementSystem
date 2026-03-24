package com.oryzem.programmanagementsystem.platform.auth.api;

public record LogoutResponse(String status) {

    static LogoutResponse signedOut() {
        return new LogoutResponse("SIGNED_OUT");
    }
}
