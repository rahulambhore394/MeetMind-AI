package com.meetmind.meetmind_backend.auth.dto;


public class LoginResponse {

    private String accessToken;
    private Long userId;
    private String name;
    private String email;

    public LoginResponse(
            String accessToken,
            Long userId,
            String name,
            String email
    ) {
        this.accessToken = accessToken;
        this.userId = userId;
        this.name = name;
        this.email = email;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public Long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }
}
