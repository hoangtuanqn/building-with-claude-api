package com.mstsoftware.rag.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.mstsoftware.rag.models.ConversationMessage;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ClaudeService {
    private final AnthropicClient client;
    private static final String MODEL = "claude-sonnet-4-6";
    private static final Long MAX_TOKENS = 1000L;

    public void addUserMessage(List<ConversationMessage> messages, String text) {
        messages.add(new ConversationMessage("user", text));
    }

    public void addAssistantMessage(List<ConversationMessage> messages, String text) {
        messages.add(new ConversationMessage("assistant", text));
    }

    public String chat(List<ConversationMessage> messages) {
        List<MessageParam> params = messages.stream()
                .map(
                        msg -> MessageParam.builder()
                        .role(MessageParam.Role.of(msg.role()))
                        .content(msg.content())
                        .build())
                .toList();

        MessageCreateParams request = MessageCreateParams.builder()
                .model(MODEL)
                .maxTokens(1000L)
                .messages(params)
                .build();

        Message response = client.messages().create(request);
        return response.content().stream()
                .filter(block -> block.type().toString().equals("text"))
                .findFirst()                                                            
                .map(block -> block.asText().text())
                .orElse("");

    }

    public Message ask(String userMessage) {
        MessageCreateParams params = MessageCreateParams.builder()
                .model(MODEL)
                .maxTokens(MAX_TOKENS)
                .addUserMessage(userMessage)
                .build();

        Message response = client.messages().create(params);
        return response;
    }
}
