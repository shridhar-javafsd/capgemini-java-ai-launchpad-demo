package com.launchpad.demo.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
 * JDBC example (history survives restart, query the H2 console to prove it):
 *   curl "http://localhost:8080/api/chat/memory/jdbc?conversationId=user-1&message=My+name+is+Vaman"
 *   curl "http://localhost:8080/api/chat/memory/jdbc?conversationId=user-2&message=What+is+my+name?"
 *   (user-2 gets no memory of "Vaman" - separate conversation)
 */
@RestController
public class MemoryChatController {

    private final ChatClient inMemoryChatClient;
    private final ChatClient jdbcChatClient;

    public MemoryChatController(
            @Qualifier("inMemoryChatClient") ChatClient inMemoryChatClient,
            @Qualifier("jdbcChatClient") ChatClient jdbcChatClient) {
        this.inMemoryChatClient = inMemoryChatClient;
        this.jdbcChatClient = jdbcChatClient;
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
}
