package com.openisle.service;

import com.openisle.dto.ConversationDetailDto;
import com.openisle.dto.ConversationDto;
import com.openisle.dto.MessageDto;
import com.openisle.dto.MessageNotificationPayload;
import com.openisle.dto.ReactionDto;
import com.openisle.dto.UserSummaryDto;
import com.openisle.mapper.ReactionMapper;
import com.openisle.model.Message;
import com.openisle.model.MessageConversation;
import com.openisle.model.MessageParticipant;
import com.openisle.model.Reaction;
import com.openisle.model.User;
import com.openisle.repository.MessageConversationRepository;
import com.openisle.repository.MessageParticipantRepository;
import com.openisle.repository.MessageRepository;
import com.openisle.repository.ReactionRepository;
import com.openisle.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

  private final MessageRepository messageRepository;
  private final MessageConversationRepository conversationRepository;
  private final MessageParticipantRepository participantRepository;
  private final UserRepository userRepository;
  private final NotificationProducer notificationProducer;
  private final ReactionRepository reactionRepository;
  private final ReactionMapper reactionMapper;

  @Transactional
  public Message sendMessage(Long senderId, Long recipientId, String content, Long replyToId) {
    log.info("Attempting to send message from user {} to user {}", senderId, recipientId);
    User sender = userRepository
      .findById(senderId)
      .orElseThrow(() -> new IllegalArgumentException("Sender not found"));
    User recipient = userRepository
      .findById(recipientId)
      .orElseThrow(() -> new IllegalArgumentException("Recipient not found"));

    log.info(
      "Finding or creating conversation for users {} and {}",
      sender.getUsername(),
      recipient.getUsername()
    );
    MessageConversation conversation = findOrCreateConversation(sender, recipient);
    log.info("Conversation found or created with ID: {}", conversation.getId());

    Message message = new Message();
    message.setConversation(conversation);
    message.setSender(sender);
    message.setContent(content);
    if (replyToId != null) {
      Message replyTo = messageRepository
        .findById(replyToId)
        .orElseThrow(() -> new IllegalArgumentException("Message not found"));
      message.setReplyTo(replyTo);
    }
    message = messageRepository.save(message);
    log.info("Message saved with ID: {}", message.getId());

    conversation.setLastMessage(message);
    conversationRepository.save(conversation);
    log.info(
      "Conversation {} updated with last message ID {}",
      conversation.getId(),
      message.getId()
    );

    try {
      MessageDto messageDto = toDto(message);

      long unreadCount = getUnreadMessageCount(recipientId);

      // 创建包含对话和参与者信息的完整payload
      Map<String, Object> conversationInfo = new HashMap<>();
      conversationInfo.put("id", conversation.getId());
      conversationInfo.put(
        "participants",
        conversation
          .getParticipants()
          .stream()
          .map(p -> {
            Map<String, Object> participantInfo = new HashMap<>();
            participantInfo.put("userId", p.getUser().getId());
            participantInfo.put("username", p.getUser().getUsername());
            return participantInfo;
          })
          .collect(Collectors.toList())
      );

      Map<String, Object> combinedPayload = new HashMap<>();
      combinedPayload.put("message", messageDto);
      combinedPayload.put("unreadCount", unreadCount);
      combinedPayload.put("conversation", conversationInfo);
      combinedPayload.put("senderId", senderId);
      if (notificationProducer != null) {
        log.info("NotificationProducer is available");
      } else {
        log.info("ERROR: NotificationProducer is NULL!");
        return message;
      }
      log.info("Recipient username: {}", recipient.getUsername());

      notificationProducer.sendNotification(
        new MessageNotificationPayload(recipient.getUsername(), combinedPayload)
      );
      log.info("=== Notification call completed ===");
    } catch (Exception e) {
      log.error("=== Error in notification process ===", e);
    }

    return message;
  }

  @Transactional
  public Message sendMessageToConversation(
    Long senderId,
    Long conversationId,
    String content,
    Long replyToId
  ) {
    User sender = userRepository
      .findById(senderId)
      .orElseThrow(() -> new IllegalArgumentException("Sender not found"));
    MessageConversation conversation = conversationRepository
      .findByIdWithParticipantsAndUsers(conversationId)
      .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

    // Join the conversation if not already a participant (useful for channels)
    participantRepository
      .findByConversationIdAndUserId(conversationId, senderId)
      .orElseGet(() -> {
        MessageParticipant p = new MessageParticipant();
        p.setConversation(conversation);
        p.setUser(sender);
        return participantRepository.save(p);
      });

    Message message = new Message();
    message.setConversation(conversation);
    message.setSender(sender);
    message.setContent(content);
    if (replyToId != null) {
      Message replyTo = messageRepository
        .findById(replyToId)
        .orElseThrow(() -> new IllegalArgumentException("Message not found"));
      message.setReplyTo(replyTo);
    }
    message = messageRepository.save(message);

    conversation.setLastMessage(message);
    conversationRepository.save(conversation);

    MessageDto messageDto = toDto(message);

    // Build participant payloads once to avoid duplicate broadcasts
    java.util.List<Map<String, Object>> participantInfos = conversation
      .getParticipants()
      .stream()
      .filter(p -> !p.getUser().getId().equals(senderId))
      .map(p -> {
        Map<String, Object> info = new HashMap<>();
        info.put("userId", p.getUser().getId());
        info.put("username", p.getUser().getUsername());
        info.put("unreadCount", getUnreadMessageCount(p.getUser().getId()));
        info.put("channelUnread", getUnreadChannelCount(p.getUser().getId()));
        return info;
      })
      .collect(Collectors.toList());

    Map<String, Object> conversationInfo = new HashMap<>();
    conversationInfo.put("id", conversation.getId());
    conversationInfo.put("participants", participantInfos);

    Map<String, Object> combinedPayload = new HashMap<>();
    combinedPayload.put("message", messageDto);
    combinedPayload.put("conversation", conversationInfo);
    combinedPayload.put("senderId", senderId);

    // Use sender's username for sharding; only one notification is needed
    notificationProducer.sendNotification(
      new MessageNotificationPayload(sender.getUsername(), combinedPayload)
    );

    return message;
  }

  public MessageDto toDto(Message message) {
    MessageDto dto = new MessageDto();
    dto.setId(message.getId());
    dto.setContent(message.getContent());
    dto.setConversationId(message.getConversation().getId());
    dto.setCreatedAt(message.getCreatedAt());

    UserSummaryDto userSummaryDto = new UserSummaryDto();
    userSummaryDto.setId(message.getSender().getId());
    userSummaryDto.setUsername(message.getSender().getUsername());
    userSummaryDto.setAvatar(message.getSender().getAvatar());
    userSummaryDto.setBot(message.getSender().isBot());
    dto.setSender(userSummaryDto);

    if (message.getReplyTo() != null) {
      Message reply = message.getReplyTo();
      MessageDto replyDto = new MessageDto();
      replyDto.setId(reply.getId());
      replyDto.setContent(reply.getContent());
      UserSummaryDto replySender = new UserSummaryDto();
      replySender.setId(reply.getSender().getId());
      replySender.setUsername(reply.getSender().getUsername());
      replySender.setAvatar(reply.getSender().getAvatar());
      replySender.setBot(reply.getSender().isBot());
      replyDto.setSender(replySender);
      dto.setReplyTo(replyDto);
    }

    java.util.List<Reaction> reactions = reactionRepository.findByMessage(message);
    java.util.List<ReactionDto> reactionDtos = reactions
      .stream()
      .map(reactionMapper::toDto)
      .collect(Collectors.toList());
    dto.setReactions(reactionDtos);

    return dto;
  }

  public MessageConversation findOrCreateConversation(Long user1Id, Long user2Id) {
    User user1 = userRepository
      .findById(user1Id)
      .orElseThrow(() -> new IllegalArgumentException("User1 not found"));
    User user2 = userRepository
      .findById(user2Id)
      .orElseThrow(() -> new IllegalArgumentException("User2 not found"));
    return findOrCreateConversation(user1, user2);
  }

  private MessageConversation findOrCreateConversation(User user1, User user2) {
    log.info(
      "Searching for existing conversation between {} and {}",
      user1.getUsername(),
      user2.getUsername()
    );
    return conversationRepository
      .findConversationsByUsers(user1, user2)
      .stream()
      .findFirst()
      .orElseGet(() -> {
        log.info("No existing conversation found. Creating a new one.");
        MessageConversation conversation = new MessageConversation();
        conversation = conversationRepository.save(conversation);
        log.info("New conversation created with ID: {}", conversation.getId());

        MessageParticipant participant1 = new MessageParticipant();
        participant1.setConversation(conversation);
        participant1.setUser(user1);
        participantRepository.save(participant1);
        log.info(
          "Participant {} added to conversation {}",
          user1.getUsername(),
          conversation.getId()
        );

        MessageParticipant participant2 = new MessageParticipant();
        participant2.setConversation(conversation);
        participant2.setUser(user2);
        participantRepository.save(participant2);
        log.info(
          "Participant {} added to conversation {}",
          user2.getUsername(),
          conversation.getId()
        );

        return conversation;
      });
  }

  @Transactional(readOnly = true)
  public List<ConversationDto> getConversations(Long userId) {
    List<MessageConversation> conversations =
      conversationRepository.findConversationsByUserIdOrderByLastMessageDesc(userId);
    return conversations
      .stream()
      .filter(c -> !c.isChannel())
      .map(c -> toDto(c, userId))
      .collect(Collectors.toList());
  }

  private ConversationDto toDto(MessageConversation conversation, Long userId) {
    ConversationDto dto = new ConversationDto();
    dto.setId(conversation.getId());
    dto.setChannel(conversation.isChannel());
    dto.setName(conversation.getName());
    dto.setAvatar(conversation.getAvatar());
    dto.setCreatedAt(conversation.getCreatedAt());
    if (conversation.getLastMessage() != null) {
      dto.setLastMessage(toDto(conversation.getLastMessage()));
    }
    dto.setParticipants(
      conversation
        .getParticipants()
        .stream()
        .map(p -> {
          UserSummaryDto userDto = new UserSummaryDto();
          userDto.setId(p.getUser().getId());
          userDto.setUsername(p.getUser().getUsername());
          userDto.setAvatar(p.getUser().getAvatar());
          userDto.setBot(p.getUser().isBot());
          return userDto;
        })
        .collect(Collectors.toList())
    );

    MessageParticipant self = conversation
      .getParticipants()
      .stream()
      .filter(p -> p.getUser().getId().equals(userId))
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("Participant not found in conversation"));

    LocalDateTime lastRead = self.getLastReadAt() == null
      ? LocalDateTime.of(1970, 1, 1, 0, 0)
      : self.getLastReadAt();
    // 只计算别人发送给当前用户的未读消息
    long unreadCount = messageRepository.countByConversationIdAndCreatedAtAfterAndSenderIdNot(
      conversation.getId(),
      lastRead,
      userId
    );
    dto.setUnreadCount(unreadCount);

    return dto;
  }

  @Transactional
  public ConversationDetailDto getConversationDetails(
    Long conversationId,
    Long userId,
    Pageable pageable
  ) {
    markConversationAsRead(conversationId, userId);

    MessageConversation conversation = conversationRepository
      .findById(conversationId)
      .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

    Page<Message> messagesPage = messageRepository.findByConversationId(conversationId, pageable);
    Page<MessageDto> messageDtoPage = messagesPage.map(this::toDto);

    List<UserSummaryDto> participants = conversation
      .getParticipants()
      .stream()
      .map(p -> {
        UserSummaryDto userDto = new UserSummaryDto();
        userDto.setId(p.getUser().getId());
        userDto.setUsername(p.getUser().getUsername());
        userDto.setAvatar(p.getUser().getAvatar());
        userDto.setBot(p.getUser().isBot());
        return userDto;
      })
      .collect(Collectors.toList());

    ConversationDetailDto detailDto = new ConversationDetailDto();
    detailDto.setId(conversation.getId());
    detailDto.setName(conversation.getName());
    detailDto.setChannel(conversation.isChannel());
    detailDto.setAvatar(conversation.getAvatar());
    detailDto.setParticipants(participants);
    detailDto.setMessages(messageDtoPage);

    return detailDto;
  }

  @Transactional
  public void markConversationAsRead(Long conversationId, Long userId) {
    MessageParticipant participant = participantRepository
      .findByConversationIdAndUserId(conversationId, userId)
      .orElseThrow(() -> new IllegalArgumentException("Participant not found"));
    participant.setLastReadAt(LocalDateTime.now());
    participantRepository.save(participant);
  }

  @Transactional(readOnly = true)
  public long getUnreadMessageCount(Long userId) {
    List<MessageParticipant> participations = participantRepository.findByUserId(userId);
    long totalUnreadCount = 0;
    for (MessageParticipant p : participations) {
      if (p.getConversation().isChannel()) continue;
      LocalDateTime lastRead = p.getLastReadAt() == null
        ? LocalDateTime.of(1970, 1, 1, 0, 0)
        : p.getLastReadAt();
      // 只计算别人发送给当前用户的未读消息
      totalUnreadCount += messageRepository.countByConversationIdAndCreatedAtAfterAndSenderIdNot(
        p.getConversation().getId(),
        lastRead,
        userId
      );
    }
    return totalUnreadCount;
  }

  @Transactional(readOnly = true)
  public long getUnreadChannelCount(Long userId) {
    List<MessageParticipant> participations = participantRepository.findByUserId(userId);
    long unreadChannelCount = 0;
    for (MessageParticipant p : participations) {
      if (!p.getConversation().isChannel()) continue;
      LocalDateTime lastRead = p.getLastReadAt() == null
        ? LocalDateTime.of(1970, 1, 1, 0, 0)
        : p.getLastReadAt();
      long unread = messageRepository.countByConversationIdAndCreatedAtAfterAndSenderIdNot(
        p.getConversation().getId(),
        lastRead,
        userId
      );
      if (unread > 0) {
        unreadChannelCount++;
      }
    }
    return unreadChannelCount;
  }
}
