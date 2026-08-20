package com.launchpad.demo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI is auto-enabled just by adding springdoc-openapi-starter-webmvc-ui to the
 * classpath (see pom.xml) - every @RestController endpoint is picked up automatically,
 * no annotations required on them. This bean only supplies the page title/description
 * shown at the top of the UI; it's optional but makes the docs look intentional rather
 * than default-generated.
 *
 * Once running:
 *   Swagger UI : http://localhost:8080/swagger-ui.html
 *   OpenAPI spec (JSON) : http://localhost:8080/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI emsAiLaunchpadOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Java AI Launchpad - EMS Spring AI Demo")
                        .description("Central chatbot, in-memory & JDBC chat memory, single/multi-user "
                                + "conversations, web-search tool calling, tool chaining, and RAG - "
                                + "backed by Spring AI over OpenAI.")
                        .version("v1.0.0")
                        .contact(new Contact().name("IBM Java Full Stack AI Launchpad")));
    }
}
