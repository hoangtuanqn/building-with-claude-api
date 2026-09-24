package com.mstsoftware.rag.controllers;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequestMapping
public class PingController {

    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> getMethodName() {
        return ResponseEntity.ok(Map.of("status", true, "message", "Pong!"));
    }

}
