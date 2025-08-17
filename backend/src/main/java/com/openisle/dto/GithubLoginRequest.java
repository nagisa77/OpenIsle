package com.openisle.dto;

import lombok.Data;

/** Request for GitHub OAuth login. */
@Data
public class GithubLoginRequest {
    private String code;
    private String redirectUri;
    private String inviteToken;
}
