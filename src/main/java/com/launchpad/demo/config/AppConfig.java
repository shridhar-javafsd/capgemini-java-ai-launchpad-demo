package com.launchpad.demo.config;

import com.launchpad.demo.tool.EmsTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    private static final String SYSTEM_PROMPT = """
            You are Ema, the assistant for the Employee Management System (EMS).
            Be concise. Use a tool when one is available and relevant instead of guessing.
            """;

    /** Demo 1: simple, stateless chatbot - no memory, no tools. */
    @Bean
    @Qualifier("simpleChatClient")
    public ChatClient simpleChatClient(OpenAiChatModel model) {
        return ChatClient.builder(model).defaultSystem(SYSTEM_PROMPT).build();
    }

    /** Demo 2: stateful chat, in-memory (lost on restart). */
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

    /** Demo 3: stateful chat, JDBC-backed (persists, per-conversationId isolation). */
    @Bean
    @Qualifier("jdbcChatClient")
    public ChatClient jdbcChatClient(OpenAiChatModel model, JdbcChatMemoryRepository jdbcRepo) {
        ChatMemory memory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(jdbcRepo)
                .maxMessages(50)
                .build();
        return ChatClient.builder(model)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build())
                .build();
    }

    /** Demo 4 & 5: web search tool + tool chaining, both bound from the same EmsTools bean.
     * The stronger, explicit instruction below matters more than it might seem - smaller
     * models in particular will often answer confidently from training data instead of
     * calling an available tool unless told plainly that guessing isn't acceptable. */
    @Bean
    @Qualifier("toolsChatClient")
    public ChatClient toolsChatClient(OpenAiChatModel model, EmsTools emsTools) {
        String toolsSystemPrompt = SYSTEM_PROMPT + """

                You have tools available: searchWeb, getEmployee, getLeaveBalance.
                For any question about current, recent, or fast-changing information -
                software versions, dates, news, prices, or anything that could be outdated
                in your training data - you MUST call searchWeb and answer from its result.
                Do not answer such questions from memory, even if you believe you know the
                answer. For employee-related questions, always call getEmployee and/or
                getLeaveBalance rather than guessing.
                """;
        return ChatClient.builder(model)
                .defaultSystem(toolsSystemPrompt)
                .defaultTools(emsTools)
                .build();
    }

    /** Demo 6: RAG - in-memory vector store, no external server required. */
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
