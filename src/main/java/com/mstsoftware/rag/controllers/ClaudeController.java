package com.mstsoftware.rag.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.anthropic.models.messages.Message;
import com.mstsoftware.rag.models.ConversationMessage;
import com.mstsoftware.rag.services.ClaudeService;

import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api")
@AllArgsConstructor
public class ClaudeController {
    private final ClaudeService claudeService;

    private final Map<String, List<ConversationMessage>> sessions = new ConcurrentHashMap<>();

    @GetMapping(value = "/chat/{sessionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter   getMethodName(@PathVariable String sessionId, @RequestParam String message) {
        SseEmitter emitter = new SseEmitter(60_000L); // timeout 60s
        List<ConversationMessage> history = sessions.computeIfAbsent(
                sessionId, k -> new ArrayList<>());

        claudeService.addUserMessage(history, message);

        // Chạy trên thread riêng, không block main thread
        Thread.ofVirtual().start(() -> {
            claudeService.chatStream(history, emitter);

            // Sau khi stream xong, lưu full response vào history
            // Cần collect lại từ stream — xem note bên dưới
        });
        return emitter;
    }

    @PostMapping("/chat/{sessionId}")
    public ResponseEntity<String> chat(
            @PathVariable String sessionId,
            @RequestBody String message) {

        List<ConversationMessage> history = sessions.computeIfAbsent(
                sessionId,
                k -> new ArrayList<>());
        claudeService.addUserMessage(history, message);
        String answer = claudeService.chat(history);
        claudeService.addAssistantMessage(history, answer);
        return ResponseEntity.ok(answer);
    }

    @DeleteMapping("/chat/{sessionId}")
    public ResponseEntity<Void> clearSession(@PathVariable String sessionId) {
        sessions.remove(sessionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/ask")
    public ResponseEntity<?> postMethodName(@RequestBody String message) {
        Message response = claudeService.ask(message);
        return ResponseEntity.ok(response);
    }

}
