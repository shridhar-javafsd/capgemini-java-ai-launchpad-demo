package com.launchpad.demo.config;

import com.launchpad.demo.tool.EmployeeTools;
import com.launchpad.demo.tool.WebSearchTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * All ChatClient variants used across the demo are built here so the
 * controllers stay thin and each concept (memory strategy, tools) is
 * visible in one place while presenting.
 */
@Configuration
public class ChatClientConfig {

    private static final String SYSTEM_PROMPT = """
            You are Ema, the assistant for the Employee Management System (EMS).
            Answer clearly and concisely. When a tool is available and relevant, use it
            instead of guessing.
            """;

    /** 1. Simple chatbot - no memory, no tools. Stateless, one-shot Q&A. */
    @Bean
    @Qualifier("simpleChatClient")
    public ChatClient simpleChatClient(OpenAiChatModel model) {
        return ChatClient.builder(model)
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }

    /** 2. Stateful conversation using in-memory chat memory (lost on restart). */
    @Bean
    @Qualifier("inMemoryChatClient")
    public ChatClient inMemoryChatClient(OpenAiChatModel model) {
        ChatMemory memory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();

        return ChatClient.builder(model)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build())
                .build();
    }

    /** 3. Stateful conversation using JDBC-backed chat memory (persists across restarts). */
    @Bean
    @Qualifier("jdbcChatClient")
    public ChatClient jdbcChatClient(OpenAiChatModel model, JdbcChatMemoryRepository jdbcChatMemoryRepository) {
        ChatMemory memory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(jdbcChatMemoryRepository)
                .maxMessages(50)
                .build();

        return ChatClient.builder(model)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build())
                .build();
    }

    /** 4. Web-search-enabled chat client - binds the WebSearchTools bean as a callable tool. */
    @Bean
    @Qualifier("webSearchChatClient")
    public ChatClient webSearchChatClient(OpenAiChatModel model, WebSearchTools webSearchTools) {
        return ChatClient.builder(model)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(webSearchTools)
                .build();
    }

    /**
     * 5. Tool-chaining chat client - binds two EmployeeTools methods. A single user
     * question (e.g. "What's the leave balance for employee E101?") forces the model
     * to call getEmployee() first to resolve context, then getLeaveBalance() - that
     * two-step call sequence is the "chaining" you present live.
     */
    @Bean
    @Qualifier("toolChainChatClient")
    public ChatClient toolChainChatClient(OpenAiChatModel model, EmployeeTools employeeTools) {
        return ChatClient.builder(model)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(employeeTools)
                .build();
    }
}
