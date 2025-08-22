package com.openisle.config;

import com.openisle.model.Channel;
import com.openisle.model.MessageConversation;
import com.openisle.repository.ChannelRepository;
import com.openisle.repository.MessageConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChannelInitializer implements CommandLineRunner {
    private final ChannelRepository channelRepository;
    private final MessageConversationRepository conversationRepository;

    @Override
    public void run(String... args) {
        if (channelRepository.count() == 0) {
            createChannel("吹水群", "闲聊讨论", "/default-avatar.svg");
            createChannel("技术讨论群", "技术交流", "/default-avatar.svg");
        }
    }

    private void createChannel(String name, String description, String avatar) {
        MessageConversation conversation = new MessageConversation();
        conversation = conversationRepository.save(conversation);
        Channel channel = new Channel();
        channel.setName(name);
        channel.setDescription(description);
        channel.setAvatar(avatar);
        channel.setConversation(conversation);
        channelRepository.save(channel);
    }
}
