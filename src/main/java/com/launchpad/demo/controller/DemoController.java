package com.launchpad.demo.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

/**
 * Every demo endpoint in one place, in the order they're presented on the call.
 * See README.md for the exact curl commands and talk points for each.
 */
@RestController
public class DemoController {

    private final ChatClient simpleChatClient;
    private final ChatClient inMemoryChatClient;
    private final ChatClient jdbcChatClient;
    private final ChatClient toolsChatClient;
    private final JdbcChatMemoryRepository jdbcChatMemoryRepository;

    public DemoController(
            @Qualifier("simpleChatClient") ChatClient simpleChatClient,
            @Qualifier("inMemoryChatClient") ChatClient inMemoryChatClient,
            @Qualifier("jdbcChatClient") ChatClient jdbcChatClient,
            @Qualifier("toolsChatClient") ChatClient toolsChatClient,
            JdbcChatMemoryRepository jdbcChatMemoryRepository) {
        this.simpleChatClient = simpleChatClient;
        this.inMemoryChatClient = inMemoryChatClient;
        this.jdbcChatClient = jdbcChatClient;
        this.toolsChatClient = toolsChatClient;
        this.jdbcChatMemoryRepository = jdbcChatMemoryRepository;
    }

    // 1. Simple chatbot
    @GetMapping("/api/chat/simple")
    public String simple(@RequestParam String message) {
        return simpleChatClient.prompt().user(message).call().content();
    }

    // 2. Stateful, in-memory
    @GetMapping("/api/chat/memory/inmemory")
    public String inMemory(@RequestParam String conversationId, @RequestParam String message) {
        return inMemoryChatClient.prompt()
                .advisors(a -> a.param(CONVERSATION_ID, conversationId))
                .user(message).call().content();
    }

    // 3. Stateful, JDBC-backed
    @GetMapping("/api/chat/memory/jdbc")
    public String jdbc(@RequestParam String conversationId, @RequestParam String message) {
        return jdbcChatClient.prompt()
                .advisors(a -> a.param(CONVERSATION_ID, conversationId))
                .user(message).call().content();
    }

    // 3b. Proof point: read persisted history straight from the DB
    @GetMapping("/api/chat/memory/history")
    public List<String> history(@RequestParam String conversationId) {
        return jdbcChatMemoryRepository.findByConversationId(conversationId).stream()
                .map(Message::getText)
                .toList();
    }

    // 4. Web search tool
    @GetMapping("/api/chat/websearch")
    public String webSearch(@RequestParam String message) {
        return toolsChatClient.prompt().user(message).call().content();
    }

    // 5. Tool chaining (same client - EMS employee tools force a two-hop call)
    @GetMapping("/api/chat/toolchain")
    public String toolChain(@RequestParam String message) {
        return toolsChatClient.prompt().user(message).call().content();
    }
}
