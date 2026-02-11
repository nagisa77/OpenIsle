package com.openisle.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

import com.openisle.dto.MessageNotificationPayload;
import com.openisle.dto.ReactionDto;
import com.openisle.mapper.ReactionMapper;
import com.openisle.model.*;
import com.openisle.repository.*;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ReactionServiceTest {

  @Test
  void reactToPostCreatesNotificationForAuthor() {
    ReactionRepository reactionRepo = mock(ReactionRepository.class);
    UserRepository userRepo = mock(UserRepository.class);
    PostRepository postRepo = mock(PostRepository.class);
    CommentRepository commentRepo = mock(CommentRepository.class);
    MessageRepository messageRepo = mock(MessageRepository.class);
    NotificationService notif = mock(NotificationService.class);
    NotificationProducer notificationProducer = mock(NotificationProducer.class);
    ReactionMapper reactionMapper = new ReactionMapper();
    EmailSender email = mock(EmailSender.class);
    ReactionService service = new ReactionService(
      reactionRepo,
      userRepo,
      postRepo,
      commentRepo,
      messageRepo,
      notif,
      notificationProducer,
      reactionMapper,
      email
    );
    User user = new User();
    user.setId(1L);
    user.setUsername("bob");
    User author = new User();
    author.setId(2L);
    author.setEmail("a@a.com");
    Post post = new Post();
    post.setId(3L);
    post.setAuthor(author);

    when(userRepo.findByUsername("bob")).thenReturn(Optional.of(user));
    when(postRepo.findById(3L)).thenReturn(Optional.of(post));
    when(reactionRepo.findByUserAndPostAndType(user, post, ReactionType.LIKE)).thenReturn(
      Optional.empty()
    );
    when(reactionRepo.save(any(Reaction.class))).thenAnswer(i -> i.getArgument(0));

    service.reactToPost("bob", 3L, ReactionType.LIKE);

    verify(notif).createNotification(
      eq(author),
      eq(NotificationType.REACTION),
      eq(post),
      isNull(),
      isNull(),
      eq(user),
      eq(ReactionType.LIKE),
      isNull()
    );
    verifyNoInteractions(email);
  }

  @Test
  void reactToMessageBroadcastsAddedEvent() {
    ReactionRepository reactionRepo = mock(ReactionRepository.class);
    UserRepository userRepo = mock(UserRepository.class);
    PostRepository postRepo = mock(PostRepository.class);
    CommentRepository commentRepo = mock(CommentRepository.class);
    MessageRepository messageRepo = mock(MessageRepository.class);
    NotificationService notif = mock(NotificationService.class);
    NotificationProducer notificationProducer = mock(NotificationProducer.class);
    ReactionMapper reactionMapper = new ReactionMapper();
    EmailSender email = mock(EmailSender.class);
    ReactionService service = new ReactionService(
      reactionRepo,
      userRepo,
      postRepo,
      commentRepo,
      messageRepo,
      notif,
      notificationProducer,
      reactionMapper,
      email
    );

    User user = new User();
    user.setId(10L);
    user.setUsername("alice");
    MessageConversation conversation = new MessageConversation();
    conversation.setId(20L);
    Message message = new Message();
    message.setId(30L);
    message.setConversation(conversation);

    when(userRepo.findByUsername("alice")).thenReturn(Optional.of(user));
    when(messageRepo.findById(30L)).thenReturn(Optional.of(message));
    when(reactionRepo.findByUserAndMessageAndType(user, message, ReactionType.LIKE)).thenReturn(
      Optional.empty()
    );
    when(reactionRepo.save(any(Reaction.class))).thenAnswer(invocation -> {
      Reaction saved = invocation.getArgument(0);
      saved.setId(40L);
      return saved;
    });

    Reaction result = service.reactToMessage("alice", 30L, ReactionType.LIKE);

    assertEquals(40L, result.getId());
    ArgumentCaptor<MessageNotificationPayload> payloadCaptor = ArgumentCaptor.forClass(
      MessageNotificationPayload.class
    );
    verify(notificationProducer).sendNotification(payloadCaptor.capture());

    MessageNotificationPayload outbound = payloadCaptor.getValue();
    assertEquals("alice", outbound.getTargetUsername());

    Object payloadObject = outbound.getPayload();
    assertInstanceOf(Map.class, payloadObject);
    Map<?, ?> payload = (Map<?, ?>) payloadObject;
    assertEquals("MESSAGE_REACTION", payload.get("eventType"));
    assertEquals(20L, payload.get("conversationId"));
    assertEquals(30L, payload.get("messageId"));
    assertEquals("ADDED", payload.get("action"));

    Object reactionObject = payload.get("reaction");
    assertInstanceOf(ReactionDto.class, reactionObject);
    ReactionDto reactionDto = (ReactionDto) reactionObject;
    assertEquals(40L, reactionDto.getId());
    assertEquals("alice", reactionDto.getUser());
    assertEquals(30L, reactionDto.getMessageId());
    assertEquals(ReactionType.LIKE, reactionDto.getType());
  }

  @Test
  void reactToMessageBroadcastsRemovedEvent() {
    ReactionRepository reactionRepo = mock(ReactionRepository.class);
    UserRepository userRepo = mock(UserRepository.class);
    PostRepository postRepo = mock(PostRepository.class);
    CommentRepository commentRepo = mock(CommentRepository.class);
    MessageRepository messageRepo = mock(MessageRepository.class);
    NotificationService notif = mock(NotificationService.class);
    NotificationProducer notificationProducer = mock(NotificationProducer.class);
    ReactionMapper reactionMapper = new ReactionMapper();
    EmailSender email = mock(EmailSender.class);
    ReactionService service = new ReactionService(
      reactionRepo,
      userRepo,
      postRepo,
      commentRepo,
      messageRepo,
      notif,
      notificationProducer,
      reactionMapper,
      email
    );

    User user = new User();
    user.setId(10L);
    user.setUsername("alice");
    MessageConversation conversation = new MessageConversation();
    conversation.setId(20L);
    Message message = new Message();
    message.setId(30L);
    message.setConversation(conversation);
    Reaction existing = new Reaction();
    existing.setId(50L);
    existing.setUser(user);
    existing.setMessage(message);
    existing.setType(ReactionType.LIKE);

    when(userRepo.findByUsername("alice")).thenReturn(Optional.of(user));
    when(messageRepo.findById(30L)).thenReturn(Optional.of(message));
    when(reactionRepo.findByUserAndMessageAndType(user, message, ReactionType.LIKE)).thenReturn(
      Optional.of(existing)
    );

    Reaction result = service.reactToMessage("alice", 30L, ReactionType.LIKE);

    assertNull(result);
    verify(reactionRepo).delete(existing);
    ArgumentCaptor<MessageNotificationPayload> payloadCaptor = ArgumentCaptor.forClass(
      MessageNotificationPayload.class
    );
    verify(notificationProducer).sendNotification(payloadCaptor.capture());

    MessageNotificationPayload outbound = payloadCaptor.getValue();
    assertEquals("alice", outbound.getTargetUsername());

    Object payloadObject = outbound.getPayload();
    assertInstanceOf(Map.class, payloadObject);
    Map<?, ?> payload = (Map<?, ?>) payloadObject;
    assertEquals("MESSAGE_REACTION", payload.get("eventType"));
    assertEquals(20L, payload.get("conversationId"));
    assertEquals(30L, payload.get("messageId"));
    assertEquals("REMOVED", payload.get("action"));

    Object reactionObject = payload.get("reaction");
    assertInstanceOf(ReactionDto.class, reactionObject);
    ReactionDto reactionDto = (ReactionDto) reactionObject;
    assertEquals(50L, reactionDto.getId());
    assertEquals("alice", reactionDto.getUser());
    assertEquals(30L, reactionDto.getMessageId());
    assertEquals(ReactionType.LIKE, reactionDto.getType());
  }
}
