package com.launchpad.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Java AI Launchpad - trainer evaluation demo.
 *
 * One Spring Boot app that demonstrates, end to end:
 *  1. A simple chatbot
 *  2. Stateful conversation with in-memory chat memory
 *  3. Stateful conversation with JDBC-backed chat memory (single + multi-user)
 *  4. A web search tool
 *  5. Tool chaining (two @Tool methods invoked in one turn)
 *  6. RAG over EMS HR policy documents using ChromaDB
 *
 * Everything is wired around the same Employee Management System (EMS) domain
 * used across the IBM Cloud Full Stack courseware, so this doubles as a
 * teaching example, not just a technical checkbox exercise.
 */
@SpringBootApplication
public class JavaAiLaunchpadDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(JavaAiLaunchpadDemoApplication.class, args);
    }
}
