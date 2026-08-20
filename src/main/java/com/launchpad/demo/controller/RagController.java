package com.launchpad.demo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Demo 6: RAG query endpoint, backed by the in-memory SimpleVectorStore.
 * curl "http://localhost:8080/api/rag/ask?question=How+many+days+of+sick+leave+do+I+get?"
 */
@RestController
@Tag(name = "RAG Demo", description = "Retrieval-augmented answers grounded in leave-policy.md")
public class RagController {

    private final ChatClient ragChatClient;

    public RagController(OpenAiChatModel model, VectorStore vectorStore) {
        this.ragChatClient = ChatClient.builder(model)
                .defaultSystem("Answer using only the retrieved EMS policy context. "
                        + "If the answer isn't in the context, say you don't have that information.")
                .defaultAdvisors(QuestionAnswerAdvisor.builder(vectorStore).build())
                .build();
    }

    @Operation(summary = "6. RAG", description = "Answers grounded in leave-policy.md via the in-memory vector store.")
    @GetMapping("/api/rag/ask")
    public String ask(@Parameter(description = "Question about EMS leave policy") @RequestParam String question) {
        return ragChatClient.prompt().user(question).call().content();
    }
}
