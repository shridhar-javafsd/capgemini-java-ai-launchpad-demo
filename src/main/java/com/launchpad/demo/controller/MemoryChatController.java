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
 * Demo point 2 & 3: Stateful conversation, in-memory and JDBC-backed, for
 * single AND multiple users.
 *
 * The "user isolation" story is entirely carried by conversationId: give two
 * different users two different conversationId values and their chat
 * histories never mix - that's the live proof point to show, not a config
 * flag.
 *
 * In-memory example (history lost on app restart):
 *   curl "http://localhost:8080/api/chat/memory/inmemory?conversationId=user-1&message=My+name+is+Vaman"
 *   curl "http://localhost:8080/api/chat/memory/inmemory?conversationId=user-1&message=What+is+my+name?"
 *
 * JDBC example (history survives restart, proven via the /history endpoint below):
 *   curl "http://localhost:8080/api/chat/memory/jdbc?conversationId=user-1&message=My+name+is+Vaman"
 *   curl "http://localhost:8080/api/chat/memory/jdbc?conversationId=user-2&message=What+is+my+name?"
 *   (user-2 gets no memory of "Vaman" - separate conversation)
 *   curl "http://localhost:8080/api/chat/memory/history?conversationId=user-1"
 *   curl "http://localhost:8080/api/chat/memory/history?conversationId=user-2"
 */
@RestController
public class MemoryChatController {

    private final ChatClient inMemoryChatClient;
    private final ChatClient jdbcChatClient;
    private final JdbcChatMemoryRepository jdbcChatMemoryRepository;

    public MemoryChatController(
            @Qualifier("inMemoryChatClient") ChatClient inMemoryChatClient,
            @Qualifier("jdbcChatClient") ChatClient jdbcChatClient,
            JdbcChatMemoryRepository jdbcChatMemoryRepository) {
        this.inMemoryChatClient = inMemoryChatClient;
        this.jdbcChatClient = jdbcChatClient;
        this.jdbcChatMemoryRepository = jdbcChatMemoryRepository;
    }

    @GetMapping("/api/chat/memory/inmemory")
    public String chatInMemory(@RequestParam String conversationId, @RequestParam String message) {
        return inMemoryChatClient.prompt()
                .advisors(a -> a.param(CONVERSATION_ID, conversationId))
                .user(message)
                .call()
                .content();
    }

    @GetMapping("/api/chat/memory/jdbc")
    public String chatJdbc(@RequestParam String conversationId, @RequestParam String message) {
        return jdbcChatClient.prompt()
                .advisors(a -> a.param(CONVERSATION_ID, conversationId))
                .user(message)
                .call()
                .content();
    }

    /**
     * Live proof point for the demo: reads straight from HSQLDB, bypassing the
     * model entirely, to show the persisted, conversationId-scoped rows.
     */
    @GetMapping("/api/chat/memory/history")
    public List<String> history(@RequestParam String conversationId) {
        return jdbcChatMemoryRepository.findByConversationId(conversationId).stream()
                .map(Message::getText)
                .toList();
    }
}

