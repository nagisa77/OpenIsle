package com.openisle.dto;

import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
public class ConversationDetailDto {
    private Long id;
    private List<UserSummaryDto> participants;
    private Page<MessageDto> messages;
    private ChannelDto channel;
}