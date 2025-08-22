package com.openisle.service;

import com.openisle.model.Message;
import com.openisle.model.MessageConversation;
import com.openisle.model.MessageParticipant;
import com.openisle.model.User;
import com.openisle.repository.MessageConversationRepository;
import com.openisle.repository.MessageParticipantRepository;
import com.openisle.repository.MessageRepository;
import com.openisle.repository.UserRepository;
import com.openisle.repository.ChannelRepository;
import com.openisle.model.Channel;
import com.openisle.dto.ChannelDto;
import com.openisle.dto.ConversationDetailDto;
import com.openisle.dto.ConversationDto;
import com.openisle.dto.MessageDto;
import com.openisle.dto.UserSummaryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final MessageRepository messageRepository;
    private final MessageConversationRepository conversationRepository;
    private final MessageParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final ChannelRepository channelRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public Message sendMessage(Long senderId, Long recipientId, String content) {
        log.info("Attempting to send message from user {} to user {}", senderId, recipientId);
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("Sender not found"));
        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new IllegalArgumentException("Recipient not found"));

        log.info("Finding or creating conversation for users {} and {}", sender.getUsername(), recipient.getUsername());
        MessageConversation conversation = findOrCreateConversation(sender, recipient);
        log.info("Conversation found or created with ID: {}", conversation.getId());

        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent(content);
        message = messageRepository.save(message);
        log.info("Message saved with ID: {}", message.getId());

        conversation.setLastMessage(message);
        conversationRepository.save(conversation);
        log.info("Conversation {} updated with last message ID {}", conversation.getId(), message.getId());

        // Broadcast the new message to subscribed clients
        MessageDto messageDto = toDto(message);
        String conversationDestination = "/topic/conversation/" + conversation.getId();
        messagingTemplate.convertAndSend(conversationDestination, messageDto);
        log.info("Message {} broadcasted to destination: {}", message.getId(), conversationDestination);

        // Also notify the recipient on their personal channel to update the conversation list
        String userDestination = "/topic/user/" + recipient.getId() + "/messages";
        messagingTemplate.convertAndSend(userDestination, messageDto);
        log.info("Message {} notification sent to destination: {}", message.getId(), userDestination);

        // Notify recipient of new unread count
        long unreadCount = getUnreadMessageCount(recipientId);
        log.info("Calculating unread count for user {}: {}", recipientId, unreadCount);
        
        // Send using username instead of user ID for WebSocket routing
        String recipientUsername = recipient.getUsername();
        messagingTemplate.convertAndSendToUser(recipientUsername, "/queue/unread-count", unreadCount);
        log.info("Sent unread count {} to user {} (username: {}) via WebSocket destination: /user/{}/queue/unread-count",
                unreadCount, recipientId, recipientUsername, recipientUsername);

        return message;
    }

    @Transactional
    public Message sendMessageToConversation(Long senderId, Long conversationId, String content) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("Sender not found"));
        MessageConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent(content);
        message = messageRepository.save(message);

        conversation.setLastMessage(message);
        conversationRepository.save(conversation);

        MessageDto messageDto = toDto(message);
        String conversationDestination = "/topic/conversation/" + conversation.getId();
        messagingTemplate.convertAndSend(conversationDestination, messageDto);

        conversation.getParticipants().forEach(p -> {
            if (!p.getUser().getId().equals(senderId)) {
                String userDestination = "/topic/user/" + p.getUser().getId() + "/messages";
                messagingTemplate.convertAndSend(userDestination, messageDto);
                long unreadCount = getUnreadMessageCount(p.getUser().getId());
                String recipientUsername = p.getUser().getUsername();
                messagingTemplate.convertAndSendToUser(recipientUsername, "/queue/unread-count", unreadCount);
            }
        });

        return message;
    }

    private MessageDto toDto(Message message) {
        MessageDto dto = new MessageDto();
        dto.setId(message.getId());
        dto.setContent(message.getContent());
        dto.setConversationId(message.getConversation().getId());
        dto.setCreatedAt(message.getCreatedAt());

        UserSummaryDto userSummaryDto = new UserSummaryDto();
        userSummaryDto.setId(message.getSender().getId());
        userSummaryDto.setUsername(message.getSender().getUsername());
        userSummaryDto.setAvatar(message.getSender().getAvatar());
        dto.setSender(userSummaryDto);

        return dto;
    }

    private ChannelDto toDto(Channel channel) {
        ChannelDto dto = new ChannelDto();
        dto.setId(channel.getId());
        dto.setName(channel.getName());
        dto.setDescription(channel.getDescription());
        dto.setAvatar(channel.getAvatar());
        if (channel.getConversation() != null) {
            dto.setConversationId(channel.getConversation().getId());
            dto.setMemberCount(channel.getConversation().getParticipants().size());
        }
        return dto;
    }

    public MessageConversation findOrCreateConversation(Long user1Id, Long user2Id) {
        User user1 = userRepository.findById(user1Id)
                .orElseThrow(() -> new IllegalArgumentException("User1 not found"));
        User user2 = userRepository.findById(user2Id)
                .orElseThrow(() -> new IllegalArgumentException("User2 not found"));
        return findOrCreateConversation(user1, user2);
    }

    private MessageConversation findOrCreateConversation(User user1, User user2) {
        log.info("Searching for existing conversation between {} and {}", user1.getUsername(), user2.getUsername());
        return conversationRepository.findConversationByUsers(user1, user2)
                .orElseGet(() -> {
                    log.info("No existing conversation found. Creating a new one.");
                    MessageConversation conversation = new MessageConversation();
                    conversation = conversationRepository.save(conversation);
                    log.info("New conversation created with ID: {}", conversation.getId());

                    MessageParticipant participant1 = new MessageParticipant();
                    participant1.setConversation(conversation);
                    participant1.setUser(user1);
                    participantRepository.save(participant1);
                    log.info("Participant {} added to conversation {}", user1.getUsername(), conversation.getId());

                    MessageParticipant participant2 = new MessageParticipant();
                    participant2.setConversation(conversation);
                    participant2.setUser(user2);
                    participantRepository.save(participant2);
                    log.info("Participant {} added to conversation {}", user2.getUsername(), conversation.getId());

                    return conversation;
                });
    }

    @Transactional(readOnly = true)
    public List<ConversationDto> getConversations(Long userId) {
        List<MessageConversation> conversations = conversationRepository.findConversationsByUserIdOrderByLastMessageDesc(userId);
        return conversations.stream().map(c -> toDto(c, userId)).collect(Collectors.toList());
    }

    private ConversationDto toDto(MessageConversation conversation, Long userId) {
        ConversationDto dto = new ConversationDto();
        dto.setId(conversation.getId());
        dto.setCreatedAt(conversation.getCreatedAt());
        if (conversation.getLastMessage() != null) {
            dto.setLastMessage(toDto(conversation.getLastMessage()));
        }
        dto.setParticipants(conversation.getParticipants().stream()
                .map(p -> {
                    UserSummaryDto userDto = new UserSummaryDto();
                    userDto.setId(p.getUser().getId());
                    userDto.setUsername(p.getUser().getUsername());
                    userDto.setAvatar(p.getUser().getAvatar());
                    return userDto;
                })
                .collect(Collectors.toList()));

        channelRepository.findByConversationId(conversation.getId())
                .ifPresent(channel -> dto.setChannel(toDto(channel)));

        MessageParticipant self = conversation.getParticipants().stream()
                .filter(p -> p.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Participant not found in conversation"));

        LocalDateTime lastRead = self.getLastReadAt() == null ? LocalDateTime.of(1970, 1, 1, 0, 0) : self.getLastReadAt();
        // 只计算别人发送给当前用户的未读消息
        long unreadCount = messageRepository.countByConversationIdAndCreatedAtAfterAndSenderIdNot(conversation.getId(), lastRead, userId);
        dto.setUnreadCount(unreadCount);

        return dto;
    }

    @Transactional
    public ConversationDetailDto getConversationDetails(Long conversationId, Long userId, Pageable pageable) {
        markConversationAsRead(conversationId, userId);

        MessageConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        Page<Message> messagesPage = messageRepository.findByConversationId(conversationId, pageable);
        Page<MessageDto> messageDtoPage = messagesPage.map(this::toDto);

        List<UserSummaryDto> participants = conversation.getParticipants().stream()
                .map(p -> {
                    UserSummaryDto userDto = new UserSummaryDto();
                    userDto.setId(p.getUser().getId());
                    userDto.setUsername(p.getUser().getUsername());
                    userDto.setAvatar(p.getUser().getAvatar());
                    return userDto;
                })
                .collect(Collectors.toList());

        ConversationDetailDto detailDto = new ConversationDetailDto();
        detailDto.setId(conversation.getId());
        detailDto.setParticipants(participants);
        detailDto.setMessages(messageDtoPage);
        channelRepository.findByConversationId(conversation.getId())
                .ifPresent(channel -> detailDto.setChannel(toDto(channel)));

        return detailDto;
    }

    @Transactional
    public void markConversationAsRead(Long conversationId, Long userId) {
        MessageParticipant participant = participantRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Participant not found"));
        participant.setLastReadAt(LocalDateTime.now());
        participantRepository.save(participant);
    }

    @Transactional(readOnly = true)
    public long getUnreadMessageCount(Long userId) {
        List<MessageParticipant> participations = participantRepository.findByUserId(userId);
        long totalUnreadCount = 0;
        for (MessageParticipant p : participations) {
            LocalDateTime lastRead = p.getLastReadAt() == null ? LocalDateTime.of(1970, 1, 1, 0, 0) : p.getLastReadAt();
            // 只计算别人发送给当前用户的未读消息
            totalUnreadCount += messageRepository.countByConversationIdAndCreatedAtAfterAndSenderIdNot(p.getConversation().getId(), lastRead, userId);
        }
        return totalUnreadCount;
    }
}