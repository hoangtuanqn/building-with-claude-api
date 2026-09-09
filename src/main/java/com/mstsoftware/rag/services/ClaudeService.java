package com.mstsoftware.rag.services;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ClaudeService {
    private final AnthropicClient client;
    private static final String MODEL = "claude-sonnet-4-6";
    private static final Long MAX_TOKENS = 1000L;

    String ask(String userMessage) {
        MessageCreateParams params = MessageCreateParams.builder()
                .model(MODEL)
                .maxTokens(MAX_TOKENS)
                .addUserMessage(userMessage)
                .build();

        Message response = client.messages().create(params);

        return response.content().stream()
                .filter(block -> block.type().toString().equals("text"))
                .findFirst()
                .map(block -> block.asText().text())
                .orElse("");
    }
}
