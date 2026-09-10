package com.mstsoftware.rag.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.anthropic.models.messages.Message;
import com.mstsoftware.rag.services.ClaudeService;

import lombok.AllArgsConstructor;

import org.apache.catalina.connector.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api")
@AllArgsConstructor
public class ClaudeController {
    private final ClaudeService claudeService;

    @PostMapping("/ask")
    public ResponseEntity<?> postMethodName(@RequestBody String message) {
        Message response = claudeService.ask(message);
        return ResponseEntity.ok(response);
    }

}
