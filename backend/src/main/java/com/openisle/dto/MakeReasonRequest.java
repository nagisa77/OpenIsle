package com.openisle.dto;

import lombok.Data;

/** Request to submit a reason (e.g., for moderation). */
@Data
public class MakeReasonRequest {
    private String token;
    private String reason;
    private String inviteToken;
}
