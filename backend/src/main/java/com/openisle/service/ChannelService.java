package com.openisle.service;

import com.openisle.dto.ChannelDto;
import com.openisle.dto.MessageDto;
import com.openisle.dto.UserSummaryDto;
import com.openisle.model.Message;
import com.openisle.model.MessageConversation;
import com.openisle.model.MessageParticipant;
import com.openisle.model.User;
import com.openisle.repository.MessageConversationRepository;
import com.openisle.repository.MessageParticipantRepository;
import com.openisle.repository.MessageRepository;
import com.openisle.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChannelService {

  private final MessageConversationRepository conversationRepository;
  private final MessageParticipantRepository participantRepository;
  private final MessageRepository messageRepository;
  private final UserRepository userRepository;

  @Transactional(readOnly = true)
  public List<ChannelDto> listChannels(Long userId) {
    List<MessageConversation> channels = conversationRepository.findByChannelTrue();
    return channels
      .stream()
      .map(c -> toDto(c, userId))
      .collect(Collectors.toList());
  }

  @Transactional
  public ChannelDto joinChannel(Long channelId, Long userId) {
    MessageConversation channel = conversationRepository
      .findById(channelId)
      .orElseThrow(() -> new IllegalArgumentException("Channel not found"));
    User user = userRepository
      .findById(userId)
      .orElseThrow(() -> new IllegalArgumentException("User not found"));
    participantRepository
      .findByConversationIdAndUserId(channelId, userId)
      .orElseGet(() -> {
        MessageParticipant p = new MessageParticipant();
        p.setConversation(channel);
        p.setUser(user);
        MessageParticipant saved = participantRepository.save(p);
        channel.getParticipants().add(saved);
        return saved;
      });
    return toDto(channel, userId);
  }

  private ChannelDto toDto(MessageConversation channel, Long userId) {
    ChannelDto dto = new ChannelDto();
    dto.setId(channel.getId());
    dto.setName(channel.getName());
    dto.setDescription(channel.getDescription());
    dto.setAvatar(channel.getAvatar());
    if (channel.getLastMessage() != null) {
      dto.setLastMessage(toMessageDto(channel.getLastMessage()));
    }
    dto.setMemberCount(channel.getParticipants().size());
    boolean joined = channel
      .getParticipants()
      .stream()
      .anyMatch(p -> p.getUser().getId().equals(userId));
    dto.setJoined(joined);
    if (joined) {
      MessageParticipant participant = channel
        .getParticipants()
        .stream()
        .filter(p -> p.getUser().getId().equals(userId))
        .findFirst()
        .orElse(null);
      LocalDateTime lastRead = participant.getLastReadAt() == null
        ? LocalDateTime.of(1970, 1, 1, 0, 0)
        : participant.getLastReadAt();
      long unread = messageRepository.countByConversationIdAndCreatedAtAfterAndSenderIdNot(
        channel.getId(),
        lastRead,
        userId
      );
      dto.setUnreadCount(unread);
    } else {
      dto.setUnreadCount(0);
    }
    return dto;
  }

  private MessageDto toMessageDto(Message message) {
    MessageDto dto = new MessageDto();
    dto.setId(message.getId());
    dto.setContent(message.getContent());
    dto.setConversationId(message.getConversation().getId());
    dto.setCreatedAt(message.getCreatedAt());

    UserSummaryDto userDto = new UserSummaryDto();
    userDto.setId(message.getSender().getId());
    userDto.setUsername(message.getSender().getUsername());
    userDto.setAvatar(message.getSender().getAvatar());
    userDto.setBot(message.getSender().isBot());
    dto.setSender(userDto);

    return dto;
  }
}
