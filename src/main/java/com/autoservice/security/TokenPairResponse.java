package com.autoservice.security;

public class TokenPairResponse {
    private final String accessToken;
    private final String refreshToken;

    public TokenPairResponse(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
}
