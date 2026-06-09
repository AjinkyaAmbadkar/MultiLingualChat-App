package com.multilingual.chat.app.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for POST /auth/google.
 *
 * The frontend uses Google Identity Services (One Tap / Sign-In button) to obtain
 * a Google ID Token (a signed JWT) in the browser. It then sends that token here.
 * The server verifies it with Google's tokeninfo endpoint and, if valid, issues
 * our own access + refresh token pair.
 *
 * The ID token is short-lived (~1 hour) and is NOT a secret per se — it is a
 * signed assertion from Google. But it should only be sent over HTTPS and consumed
 * once (our backend doesn't need to store it).
 */
public class GoogleLoginRequestDto {

    @NotBlank(message = "Google ID token must not be blank")
    private String idToken;

    public GoogleLoginRequestDto() {}

    public GoogleLoginRequestDto(String idToken) {
        this.idToken = idToken;
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }
}
