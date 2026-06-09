package com.multilingual.chat.app.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for POST /auth/refresh and POST /auth/logout.
 * Client sends the refresh token it received at login.
 */
public class RefreshTokenRequestDto {

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;

    public RefreshTokenRequestDto() {
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
