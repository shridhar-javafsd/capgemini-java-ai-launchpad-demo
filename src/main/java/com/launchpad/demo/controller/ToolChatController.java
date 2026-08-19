package com.launchpad.demo.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Demo point 4 (web search tool) and 5 (tool chaining), exposed as two
 * separate endpoints so each can be shown in isolation before combining them.
 *
 * curl "http://localhost:8080/api/chat/websearch?message=What+is+the+latest+Spring+AI+release?"
 * curl "http://localhost:8080/api/chat/toolchain?message=What+is+the+leave+balance+for+employee+E102?"
 */
@RestController
public class ToolChatController {

    private final ChatClient webSearchChatClient;
    private final ChatClient toolChainChatClient;

    public ToolChatController(
            @Qualifier("webSearchChatClient") ChatClient webSearchChatClient,
            @Qualifier("toolChainChatClient") ChatClient toolChainChatClient) {
        this.webSearchChatClient = webSearchChatClient;
        this.toolChainChatClient = toolChainChatClient;
    }

    @GetMapping("/api/chat/websearch")
    public String webSearch(@RequestParam String message) {
        return webSearchChatClient.prompt()
                .user(message)
                .call()
                .content();
    }

    @GetMapping("/api/chat/toolchain")
    public String toolChain(@RequestParam String message) {
        return toolChainChatClient.prompt()
                .user(message)
                .call()
                .content();
    }
}
