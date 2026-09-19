package com.mstsoftware.rag.services;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.http.StreamResponse;
import com.anthropic.helpers.MessageAccumulator;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.RawMessageStreamEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mstsoftware.rag.models.ConversationMessage;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ClaudeService {
    private final AnthropicClient client;
    private final ObjectMapper objectMapper;
    private static final String MODEL = "claude-sonnet-4-6";
    private static final Long MAX_TOKENS = 1000L;
    private static final String SYSTEM_PROMPT = "Bạn là một giáo viên dạy toán kiên nhẫn. Đừng trả lời trực tiếp các câu hỏi của học sinh. Hãy hướng dẫn các em tìm ra lời giải từng bước một.";
    private static final double TEMPERATURE = 0.0;

    public void addUserMessage(List<ConversationMessage> messages, String text) {
        messages.add(new ConversationMessage("user", text));
    }

    public void addAssistantMessage(List<ConversationMessage> messages, String text) {
        messages.add(new ConversationMessage("assistant", text));
    }

    // hỏi AI dạng có ghi nhớ conversation và streaming
    public void chatStream(List<ConversationMessage> messages, SseEmitter emitter) {
        MessageCreateParams request = buildRequest(messages).build();
        MessageAccumulator accumulator = MessageAccumulator.create();
        try (StreamResponse<RawMessageStreamEvent> streamResponse = client.messages().createStreaming(request)) {
            streamResponse.stream()
                    .peek(accumulator::accumulate)
                    .flatMap(event -> event.contentBlockDelta().stream())
                    .flatMap(deltaEvent -> deltaEvent.delta().text().stream())
                    .forEach(textDelta -> {
                        try {
                            emitter.send(textDelta.text());
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                    });

            // Lưu full response vào history
            String fullText = accumulator.message().content().stream()
                    .filter(block -> block.type().toString().equals("text"))
                    .findFirst()
                    .map(block -> block.asText().text())
                    .orElse("");

            addAssistantMessage(messages, fullText);
            emitter.complete();
        } catch (Exception ex) {
            emitter.completeWithError(ex);
        }
    }

    // hỏi AI dạng có ghi nhớ conversation
    public String chat(List<ConversationMessage> messages, List<String> stopSequences) {
        MessageCreateParams.Builder builder = buildRequest(messages);

        if (stopSequences != null && !stopSequences.isEmpty()) {
            builder.stopSequences(stopSequences);
        }

        Message response = client.messages().create(builder.build());
        return response.content().stream()
                .filter(block -> block.type().toString().equals("text"))
                .findFirst()
                .map(block -> block.asText().text())
                .orElse("");

    }

    // hỏi AI dạng ko ghi nhớ conversation
    public Message ask(String userMessage) {
        MessageCreateParams params = MessageCreateParams.builder()
                .model(MODEL)
                .maxTokens(MAX_TOKENS)
                .addUserMessage(userMessage)
                .build();

        Message response = client.messages().create(params);
        return response;
    }

    public JsonNode chatAsJson(List<ConversationMessage> messages) throws Exception {
        addAssistantMessage(messages, "```json");
        String raw = chat(messages, List.of("```"));

        // Parse string thành JsonNode
        return objectMapper.readValue(raw.trim(), JsonNode.class);
    }

    private MessageCreateParams.Builder buildRequest(List<ConversationMessage> messages) {
        List<MessageParam> params = messages.stream()
                .map(
                        msg -> MessageParam.builder()
                                .role(MessageParam.Role.of(msg.role()))
                                .content(msg.content())
                                .build())
                .toList();
        return MessageCreateParams.builder()
                .model(MODEL)
                .maxTokens(1000L)
                .messages(params)
                .system(SYSTEM_PROMPT)
                .temperature(TEMPERATURE);

    }
}
