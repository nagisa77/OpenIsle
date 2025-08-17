package com.openisle.dto;

import lombok.Data;

/** Request to register a new user. */
@Data
public class RegisterRequest {
    private String username;
    private String email;
    private String password;
    private String captcha;
    private String inviteToken;
}
