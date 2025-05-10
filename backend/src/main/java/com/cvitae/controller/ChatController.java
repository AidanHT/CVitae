package com.cvitae.controller;

import com.cvitae.dto.ChatMessageRequest;
import com.cvitae.dto.ChatMessageResponse;
import com.cvitae.dto.SuggestionRequest;
import com.cvitae.dto.SuggestionResponse;
import com.cvitae.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
// CORS handled globally in CorsConfig.java
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/message")
    public ResponseEntity<ChatMessageResponse> sendMessage(@Valid @RequestBody ChatMessageRequest request) {
        log.info("Processing chat message for resume optimization");
        ChatMessageResponse response = chatService.processMessage(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/suggestions")
    public ResponseEntity<List<SuggestionResponse>> getSuggestions(@Valid @RequestBody SuggestionRequest request) {
        log.info("Generating AI suggestions for resume improvement");
        List<SuggestionResponse> suggestions = chatService.generateSuggestions(request);
        return ResponseEntity.ok(suggestions);
    }

    @GetMapping("/conversation/{sessionId}")
    public ResponseEntity<List<ChatMessageResponse>> getConversation(@PathVariable String sessionId) {
        log.info("Retrieving conversation history for session: {}", sessionId);
        List<ChatMessageResponse> conversation = chatService.getConversationHistory(sessionId);
        return ResponseEntity.ok(conversation);
    }

    @DeleteMapping("/conversation/{sessionId}")
    public ResponseEntity<Void> clearConversation(@PathVariable String sessionId) {
        log.info("Clearing conversation for session: {}", sessionId);
        chatService.clearConversation(sessionId);
        return ResponseEntity.noContent().build();
    }
}
