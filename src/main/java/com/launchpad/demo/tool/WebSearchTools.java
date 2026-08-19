package com.launchpad.demo.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Demo point 4: Web search as a callable tool.
 *
 * Spring AI does not ship a built-in web search tool - you register one
 * yourself, same as any other @Tool. This wraps Tavily's search API
 * (a common lightweight choice for LLM-facing search; SerpAPI or Bing
 * Search work the same way - just swap the RestClient call).
 *
 * Get a free Tavily key at https://tavily.com and set TAVILY_API_KEY.
 */
@Component
public class WebSearchTools {

    private final RestClient restClient = RestClient.create("https://api.tavily.com");

    @Value("${TAVILY_API_KEY:}")
    private String tavilyApiKey;

    @Tool(description = "Search the public web for current information not available in the model's training data, "
            + "such as recent news, prices, or fast-changing facts.")
    public String searchWeb(String query) {
        if (tavilyApiKey.isBlank()) {
            return "Web search is not configured for this demo run (no TAVILY_API_KEY set). "
                    + "In a live environment this would return live search results for: " + query;
        }

        Map<String, Object> requestBody = Map.of(
                "api_key", tavilyApiKey,
                "query", query,
                "max_results", 3
        );

        return restClient.post()
                .uri("/search")
                .body(requestBody)
                .retrieve()
                .body(String.class);
    }
}
