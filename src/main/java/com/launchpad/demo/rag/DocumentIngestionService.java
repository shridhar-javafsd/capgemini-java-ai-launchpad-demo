package com.launchpad.demo.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Loads leave-policy.md into the in-memory SimpleVectorStore on every startup.
 * Nothing external to run first - this is the whole point of using SimpleVectorStore
 * instead of ChromaDB for this demo.
 */
@Component
public class DocumentIngestionService implements CommandLineRunner {

    private final VectorStore vectorStore;
    private final ResourceLoader resourceLoader;

    public DocumentIngestionService(VectorStore vectorStore, ResourceLoader resourceLoader) {
        this.vectorStore = vectorStore;
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void run(String... args) {
        TextReader reader = new TextReader(resourceLoader.getResource("classpath:docs/leave-policy.md"));
        reader.getCustomMetadata().put("source", "ems-leave-policy");
        List<Document> chunks = new TokenTextSplitter().apply(reader.get());
        vectorStore.add(chunks);
        System.out.println("[RAG] Ingested " + chunks.size() + " chunks from leave-policy.md (in-memory store)");
    }
}
