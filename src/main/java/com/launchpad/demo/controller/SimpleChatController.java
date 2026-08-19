package com.launchpad.demo.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Demo point 1: Simple chatbot.
 * curl "http://localhost:8080/api/chat/simple?message=What+is+Spring+AI?"
 */
@RestController
public class SimpleChatController {

    private final ChatClient chatClient;

    public SimpleChatController(@Qualifier("simpleChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @GetMapping("/api/chat/simple")
    public String chat(@RequestParam String message) {
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }
}
