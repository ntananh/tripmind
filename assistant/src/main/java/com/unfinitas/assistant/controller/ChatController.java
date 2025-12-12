package com.unfinitas.assistant.controller;

import com.unfinitas.assistant.model.ChatRequest;
import com.unfinitas.assistant.model.ChatResponse;
import com.unfinitas.assistant.service.AssistantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final AssistantService assistantService;

    @PostMapping
    public ChatResponse chat(@RequestBody final ChatRequest request) {
        return assistantService.process(request.message());
    }
}
