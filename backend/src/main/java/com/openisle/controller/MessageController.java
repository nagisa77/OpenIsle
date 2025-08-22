package com.openisle.controller;

import com.openisle.dto.ConversationDetailDto;
import com.openisle.dto.ConversationDto;
import com.openisle.dto.CreateConversationRequest;
import com.openisle.dto.CreateConversationResponse;
import com.openisle.dto.MessageDto;
import com.openisle.dto.UserSummaryDto;
import com.openisle.model.Message;
import com.openisle.model.MessageConversation;
import com.openisle.model.User;
import com.openisle.repository.UserRepository;
import com.openisle.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final UserRepository userRepository;

    // This is a placeholder for getting the current user's ID
    private Long getCurrentUserId(Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow(() -> new IllegalArgumentException("Sender not found"));
        // In a real application, you would get this from the Authentication object
        return user.getId();
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationDto>> getConversations(Authentication auth) {
        List<ConversationDto> conversations = messageService.getConversations(getCurrentUserId(auth));
        return ResponseEntity.ok(conversations);
    }

    @GetMapping("/conversations/{conversationId}")
    public ResponseEntity<ConversationDetailDto> getMessages(@PathVariable Long conversationId,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "20") int size,
                                                               Authentication auth) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        ConversationDetailDto conversationDetails = messageService.getConversationDetails(conversationId, getCurrentUserId(auth), pageable);
        return ResponseEntity.ok(conversationDetails);
    }

    @PostMapping
    public ResponseEntity<MessageDto> sendMessage(@RequestBody MessageRequest req, Authentication auth) {
        Message message = messageService.sendMessage(getCurrentUserId(auth), req.getRecipientId(), req.getContent());
        return ResponseEntity.ok(toDto(message));
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<MessageDto> sendMessageToConversation(@PathVariable Long conversationId,
                                                                @RequestBody ContentRequest req,
                                                                Authentication auth) {
        Message message = messageService.sendMessageToConversation(getCurrentUserId(auth), conversationId, req.getContent());
        return ResponseEntity.ok(toDto(message));
    }

    @PostMapping("/conversations/{conversationId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long conversationId, Authentication auth) {
        messageService.markConversationAsRead(conversationId, getCurrentUserId(auth));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/conversations")
    public ResponseEntity<CreateConversationResponse> findOrCreateConversation(@RequestBody CreateConversationRequest req, Authentication auth) {
        MessageConversation conversation = messageService.findOrCreateConversation(getCurrentUserId(auth), req.getRecipientId());
        return ResponseEntity.ok(new CreateConversationResponse(conversation.getId()));
    }

    private MessageDto toDto(Message message) {
        MessageDto dto = new MessageDto();
        dto.setId(message.getId());
        dto.setContent(message.getContent());
        dto.setCreatedAt(message.getCreatedAt());

        dto.setConversationId(message.getConversation().getId());

        UserSummaryDto senderDto = new UserSummaryDto();
        senderDto.setId(message.getSender().getId());
        senderDto.setUsername(message.getSender().getUsername());
        senderDto.setAvatar(message.getSender().getAvatar());
        dto.setSender(senderDto);

        return dto;
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount(Authentication auth) {
        return ResponseEntity.ok(messageService.getUnreadMessageCount(getCurrentUserId(auth)));
    }

    // A simple request DTO
    static class MessageRequest {
        private Long recipientId;
        private String content;

        public Long getRecipientId() {
            return recipientId;
        }

        public void setRecipientId(Long recipientId) {
            this.recipientId = recipientId;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    static class ContentRequest {
        private String content;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}