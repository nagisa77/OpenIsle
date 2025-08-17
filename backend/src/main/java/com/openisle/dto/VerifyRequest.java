package com.openisle.dto;

import lombok.Data;

/** Request to verify a user registration. */
@Data
public class VerifyRequest {
    private String username;
    private String code;
    private String inviteToken;
}
