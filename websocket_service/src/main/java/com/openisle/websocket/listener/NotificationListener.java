package com.openisle.websocket.listener;

import com.openisle.websocket.dto.MessageNotificationPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.lang.Nullable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationListener {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Unified listener for all sharded queues and the backward-compatible legacy queue.
     *
     * @param payload The message payload.
     * @param queueName The name of the queue the message was consumed from. This header is optional.
     */
    @RabbitListener(
            id = "shardedListenerContainer",
            queues = {
                "notifications-queue-0", "notifications-queue-1", "notifications-queue-2", "notifications-queue-3",
                "notifications-queue-4", "notifications-queue-5", "notifications-queue-6", "notifications-queue-7",
                "notifications-queue-8", "notifications-queue-9", "notifications-queue-a", "notifications-queue-b",
                "notifications-queue-c", "notifications-queue-d", "notifications-queue-e", "notifications-queue-f",
                "notifications-queue"
            }
    )
    public void receiveMessage(MessageNotificationPayload payload, @Header("amqp_consumedQueue") @Nullable String queueName) {
        if (queueName != null) {
            String queueNamePrefix = "notifications-queue-";
            if (queueName.startsWith(queueNamePrefix)) {
                String shardIndexStr = queueName.substring(queueNamePrefix.length());
                log.info("=== RabbitMQ Message Received from Shard {} ({}) ===", shardIndexStr, queueName);
            } else {
                log.info("=== RabbitMQ Message Received from Legacy Queue ({}) ===", queueName);
            }
        }
        String username = payload.getTargetUsername();
        Object payloadObject = payload.getPayload();
        log.info("Target username: {}", username);
        log.info("Payload object type: {}", payloadObject != null ? payloadObject.getClass().getSimpleName() : "null");
        log.info("Payload content: {}", payloadObject);

        try {
            if (payloadObject instanceof Map) {
                Map<String, Object> payloadMap = (Map<String, Object>) payloadObject;

                if ("MESSAGE_REACTION".equals(payloadMap.get("eventType"))) {
                    Object conversationIdObj = payloadMap.get("conversationId");
                    if (conversationIdObj instanceof Number) {
                        Long conversationId = ((Number) conversationIdObj).longValue();
                        String conversationDestination = "/topic/conversation/" + conversationId;
                        messagingTemplate.convertAndSend(conversationDestination, payloadMap);
                        log.info("Message reaction broadcasted to destination: {}", conversationDestination);
                    } else {
                        log.warn("Missing or invalid conversationId for reaction payload: {}", payloadMap);
                    }
                }
                // 处理包含完整对话信息的消息 - 完全复制之前的WebSocket发送逻辑
                else if (payloadMap.containsKey("message") && payloadMap.containsKey("conversation") && payloadMap.containsKey("senderId")) {
                    Object messageObj = payloadMap.get("message");
                    Map<String, Object> conversationInfo = (Map<String, Object>) payloadMap.get("conversation");
                    Long conversationId = ((Number) conversationInfo.get("id")).longValue();
                    Long senderId = ((Number) payloadMap.get("senderId")).longValue();
                    List<Map<String, Object>> participants = (List<Map<String, Object>>) conversationInfo.get("participants");

                    // 1. 发送到conversation topic
                    String conversationDestination = "/topic/conversation/" + conversationId;
                    messagingTemplate.convertAndSend(conversationDestination, messageObj);
                    log.info("Message broadcasted to destination: {}", conversationDestination);

                    // 2. 为所有参与者（除发送者外）发送到个人频道和未读数量
                    for (Map<String, Object> participant : participants) {
                        Long participantUserId = ((Number) participant.get("userId")).longValue();
                        String participantUsername = (String) participant.get("username");

                        if (!participantUserId.equals(senderId)) {
                            // 发送到用户个人消息频道
                            String userDestination = "/topic/user/" + participantUserId + "/messages";
                            messagingTemplate.convertAndSend(userDestination, messageObj);
                            log.info("Message notification sent to destination: {}", userDestination);

                            // 优先从 participant 中获取未读信息，兼容旧格式
                            Object unreadCount = participant.getOrDefault("unreadCount", payloadMap.get("unreadCount"));
                            if (unreadCount != null) {
                                messagingTemplate.convertAndSendToUser(participantUsername, "/queue/unread-count", unreadCount);
                                log.info("Sent unread count to user {} via /user/{}/queue/unread-count", participantUsername, participantUsername);
                            }

                            Object channelUnread = participant.getOrDefault("channelUnread", payloadMap.get("channelUnread"));
                            if (channelUnread != null) {
                                messagingTemplate.convertAndSendToUser(participantUsername, "/queue/channel-unread", channelUnread);
                                log.info("Sent channel-unread to {}", participantUsername);
                            }
                        }
                    }
                }
                // 处理简化的消息格式（向后兼容）
                else if (payloadMap.containsKey("message")) {

                    if (payloadMap.containsKey("unreadCount")) {
                        messagingTemplate.convertAndSendToUser(username, "/queue/unread-count", payloadMap.get("unreadCount"));
                        log.info("Sent unread count to user {} via /user/{}/queue/unread-count", username, username);
                    }
                    if (payloadMap.containsKey("channelUnread")) {
                        messagingTemplate.convertAndSendToUser(username, "/queue/channel-unread", payloadMap.get("channelUnread"));
                        log.info("Sent channel-unread to {}", username);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to process and send message for user {}", username, e);
        }
    }
}
