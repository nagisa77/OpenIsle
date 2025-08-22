package com.openisle.controller;

import com.openisle.dto.ChannelDto;
import com.openisle.model.Channel;
import com.openisle.model.MessageParticipant;
import com.openisle.model.MessageConversation;
import com.openisle.model.User;
import com.openisle.repository.ChannelRepository;
import com.openisle.repository.MessageParticipantRepository;
import com.openisle.repository.UserRepository;
import com.openisle.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/channels")
@RequiredArgsConstructor
public class ChannelController {
    private final ChannelRepository channelRepository;
    private final MessageParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;

    private Long getCurrentUserId(Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow(() -> new IllegalArgumentException("User not found"));
        return user.getId();
    }

    @GetMapping
    public ResponseEntity<List<ChannelDto>> listChannels(Authentication auth) {
        Long userId = auth == null ? null : getCurrentUserId(auth);
        List<ChannelDto> channels = channelRepository.findAll().stream()
                .map(c -> toDto(c, userId))
                .collect(Collectors.toList());
        return ResponseEntity.ok(channels);
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<Void> joinChannel(@PathVariable Long id, Authentication auth) {
        Channel channel = channelRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Channel not found"));
        Long userId = getCurrentUserId(auth);
        boolean exists = channel.getConversation().getParticipants().stream().anyMatch(p -> p.getUser().getId().equals(userId));
        if (!exists) {
            MessageParticipant participant = new MessageParticipant();
            participant.setConversation(channel.getConversation());
            participant.setUser(userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found")));
            participantRepository.save(participant);
        }
        return ResponseEntity.ok().build();
    }

    private ChannelDto toDto(Channel channel, Long userId) {
        ChannelDto dto = new ChannelDto();
        dto.setId(channel.getId());
        dto.setName(channel.getName());
        dto.setDescription(channel.getDescription());
        dto.setAvatar(channel.getAvatar());
        if (channel.getConversation() != null) {
            MessageConversation conversation = channel.getConversation();
            dto.setConversationId(conversation.getId());
            dto.setMemberCount(conversation.getParticipants().size());
            if (userId != null) {
                MessageParticipant self = conversation.getParticipants().stream()
                        .filter(p -> p.getUser().getId().equals(userId))
                        .findFirst().orElse(null);
                if (self != null) {
                    var lastRead = self.getLastReadAt();
                    dto.setUnreadCount(messageRepository
                            .countByConversationIdAndCreatedAtAfterAndSenderIdNot(conversation.getId(),
                                    lastRead == null ? java.time.LocalDateTime.of(1970,1,1,0,0) : lastRead,
                                    userId));
                }
            }
        }
        return dto;
    }
}
