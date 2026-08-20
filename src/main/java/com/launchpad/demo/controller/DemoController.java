package com.launchpad.demo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * See README.md for the exact curl commands and talk points for each.
 */
@RestController
@Tag(name = "Chat Demos", description = "Central chatbot: stateless, memory, tools, tool chaining")
public class DemoController {

	private final ChatClient simpleChatClient;
	private final ChatClient inMemoryChatClient;
	private final ChatClient jdbcChatClient;
	private final ChatClient toolsChatClient;
	private final JdbcChatMemoryRepository jdbcChatMemoryRepository;

	public DemoController(@Qualifier("simpleChatClient") ChatClient simpleChatClient,
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
	@Operation(summary = "1. Simple chatbot", description = "Stateless - no memory, no tools. Each call is a clean slate.")
	@GetMapping("/api/chat/simple")
	public String simple(@Parameter(description = "The message to send") @RequestParam String message) {
		return simpleChatClient.prompt().user(message).call().content();
	}

	// 2. Stateful, in-memory
	@Operation(summary = "2. Chat with in-memory conversation history", description = "Remembers within this JVM's lifetime only, isolated per conversationId (= userId).")
	@GetMapping("/api/chat/memory/inmemory")
	public String inMemory(
			@Parameter(description = "Conversation/user id - same id keeps context, different ids are isolated") @RequestParam String conversationId,
			@Parameter(description = "The message to send") @RequestParam String message) {
		return inMemoryChatClient.prompt().advisors(a -> a.param(CONVERSATION_ID, conversationId)).user(message).call()
				.content();
	}

	// 3. Stateful, JDBC-backed
	@Operation(summary = "3. Chat with JDBC-backed conversation history", description = "Persists to HSQLDB - survives an app restart, isolated per conversationId (= userId).")
	@GetMapping("/api/chat/memory/jdbc")
	public String jdbc(
			@Parameter(description = "Conversation/user id - same id keeps context, different ids are isolated") @RequestParam String conversationId,
			@Parameter(description = "The message to send") @RequestParam String message) {
		return jdbcChatClient.prompt().advisors(a -> a.param(CONVERSATION_ID, conversationId)).user(message).call()
				.content();
	}

	// 3b. Proof point: read persisted history straight from the DB
	@Operation(summary = "3b. Read raw persisted history", description = "Proof point for the JDBC memory demo - reads straight from the SPRING_AI_CHAT_MEMORY table.")
	@GetMapping("/api/chat/memory/history")
	public List<String> history(
			@Parameter(description = "Conversation/user id to look up") @RequestParam String conversationId) {
		return jdbcChatMemoryRepository.findByConversationId(conversationId).stream().map(Message::getText).toList();
	}

	// 4. Web search tool
	@Operation(summary = "4. Web search tool calling", description = "The model decides on its own whether to call searchWeb() based on the question.")
	@GetMapping("/api/chat/websearch")
	public String webSearch(@Parameter(description = "The message to send") @RequestParam String message) {
		return toolsChatClient.prompt().user(message).call().content();
	}

	// 5. Tool chaining (same client - EMS employee tools force a two-hop call)
	@Operation(summary = "5. Tool chaining", description = "Ask something that needs two tools in one turn (e.g. web search + an employee lookup) "
			+ "to show the model calling both before replying.")
	@GetMapping("/api/chat/toolchain")
	public String toolChain(@Parameter(description = "The message to send") @RequestParam String message) {
		return toolsChatClient.prompt().user(message).call().content();
	}
}
