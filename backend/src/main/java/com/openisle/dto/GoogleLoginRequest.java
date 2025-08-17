package com.openisle.dto;

import lombok.Data;

/** Request for Google OAuth login. */
@Data
public class GoogleLoginRequest {
    private String idToken;
    private String inviteToken;
}
