package com.openisle.dto;

import lombok.Data;

/** Request for Discord OAuth login. */
@Data
public class DiscordLoginRequest {
    private String code;
    private String redirectUri;
    private String inviteToken;
}
