package com.launchpad.demo.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Demo point 6: RAG ingestion.
 *
 * Loads src/main/resources/docs/leave-policy.md, splits it into chunks, and
 * writes the embeddings into ChromaDB on startup. Point this at the ChromaDB
 * running locally (docker run -p 8000:8000 chromadb/chroma) before you boot
 * the app.
 *
 * For the live demo, walk through this class briefly, then show the RAG
 * query endpoint answering from the ingested policy text - that's the "simple
 * vector store" story made concrete with ChromaDB as the store.
 */
@Component
public class DocumentIngestionService implements CommandLineRunner {

    private final VectorStore vectorStore;
    private final Resource leavePolicyDoc;

    public DocumentIngestionService(
            VectorStore vectorStore,
            org.springframework.core.io.ResourceLoader resourceLoader) {
        this.vectorStore = vectorStore;
        this.leavePolicyDoc = resourceLoader.getResource("classpath:docs/leave-policy.md");
    }

    @Override
    public void run(String... args) {
        TextReader reader = new TextReader(leavePolicyDoc);
        reader.getCustomMetadata().put("source", "ems-leave-policy");
        List<Document> documents = reader.get();

        List<Document> chunks = new TokenTextSplitter().apply(documents);
        vectorStore.add(chunks);

        System.out.println("[RAG] Ingested " + chunks.size() + " chunks from leave-policy.md into ChromaDB");
    }
}
