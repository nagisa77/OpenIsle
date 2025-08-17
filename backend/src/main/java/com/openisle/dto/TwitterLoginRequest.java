package com.openisle.dto;

import lombok.Data;

/** Request for Twitter OAuth login. */
@Data
public class TwitterLoginRequest {
    private String code;
    private String redirectUri;
    private String codeVerifier;
    private String inviteToken;
}
