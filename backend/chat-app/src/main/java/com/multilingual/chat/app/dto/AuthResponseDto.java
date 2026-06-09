package com.multilingual.chat.app.dto;

/**
 * Returned on successful register, login, or token refresh.
 *
 * accessToken  — short-lived JWT (15 min). Client sends this in every API request:
 *                  Authorization: Bearer <accessToken>
 *
 * refreshToken — long-lived opaque token (7 days). Client sends this ONLY to
 *                POST /auth/refresh when the access token expires.
 *                Never send this to other endpoints.
 */
public class AuthResponseDto {

    private String accessToken;
    private String refreshToken;

    // Always "Bearer" — tells the client how to use the access token
    private String tokenType = "Bearer";

    private Long userId;
    private String email;
    private String name;

    public AuthResponseDto() {
    }

    public AuthResponseDto(String accessToken, String refreshToken, Long userId, String email, String name) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.userId = userId;
        this.email = email;
        this.name = name;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
