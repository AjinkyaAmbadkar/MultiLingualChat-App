package com.multilingual.chat.app.service.impl;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.multilingual.chat.app.service.TranslationService;

/**
 * Real translation service that calls the OpenAI Chat Completions API.
 *
 * @Primary tells Spring: "when multiple beans implement TranslationService,
 *          inject this one by default." This lets MockTranslationServiceImpl
 *          stay around for tests without causing a conflict.
 */
@Service
@Primary
public class OpenAiTranslationServiceImpl implements TranslationService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiTranslationServiceImpl.class);

    private final RestClient restClient;
    private final String model;

    /**
     * Constructor injection with @Value — Spring reads these from
     * application.properties,
     * which in turn reads them from environment variables.
     *
     * RestClient is Spring's modern HTTP client (replaces RestTemplate in Spring
     * 6.1+).
     * We configure it once here with the base URL and auth header so every call
     * reuses it.
     */
    public OpenAiTranslationServiceImpl(
            @Value("${openai.api.key}") String apiKey,
            @Value("${openai.api.url}") String apiUrl,
            @Value("${openai.model}") String model) {

        this.model = model;

        // RestClient.builder() — fluent builder pattern to configure a reusable HTTP
        // client
        this.restClient = RestClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey) // OpenAI auth
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Skip the API call entirely if source and target language are the same.
     * No point paying for a translation of "English → English".
     */
    @Override
    public boolean isTranslationRequired(String sourceLanguage, String targetLanguage) {
        if (sourceLanguage == null || targetLanguage == null) {
            log.debug("No Translation required as Source and Target Language is same.");
            return false;
        }
        return !sourceLanguage.trim().equalsIgnoreCase(targetLanguage.trim());
    }

    /**
     * Translates text by calling OpenAI's Chat Completions API.
     *
     * OpenAI API request shape (JSON):
     * {
     * "model": "gpt-4o-mini",
     * "messages": [
     * { "role": "system", "content": "<instructions for the model>" },
     * { "role": "user", "content": "<the actual text to translate>" }
     * ],
     * "temperature": 0.3, <-- lower = more deterministic/accurate (good for
     * translation)
     * "max_tokens": 1000
     * }
     *
     * OpenAI API response shape (JSON):
     * {
     * "choices": [
     * {
     * "message": {
     * "content": "<translated text>"
     * }
     * }
     * ]
     * }
     */
    @Override
    public String translate(String text, String sourceLanguage, String targetLanguage) {

        log.info("Requesting translation | from: {} | to: {} | text length: {} chars",
                sourceLanguage, targetLanguage, text.length());
        log.info("Calling OpenAPI API to get the translation for message");
        // Build the request body using Java Maps — Jackson (included via spring-web)
        // will automatically serialize this Map into the correct JSON structure.
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system",
                                "content", "You are a translation assistant. Translate the given text accurately. "
                                        + "Return only the translated text, nothing else. No explanations, no quotes."),
                        Map.of("role", "user",
                                "content", "Translate the following text from " + sourceLanguage
                                        + " to " + targetLanguage + ":\n\n" + text)),
                "temperature", 0.3, // low temperature = precise, consistent translations
                "max_tokens", 1000);

        try {
            // RestClient fluent call:
            // .post() → HTTP POST
            // .body(...) → serialize the Map to JSON and set as request body
            // .retrieve() → execute the request and prepare to read the response
            // .body(Map.class)→ deserialize the JSON response into a Map
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            // Navigate the response structure: choices → [0] → message → content
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");

            if (choices == null || choices.isEmpty()) {
                throw new RuntimeException("OpenAI returned an empty choices list");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");

            String translatedText = (String) message.get("content");
            log.info("Translation successful | from: {} | to: {}", sourceLanguage, targetLanguage);
            log.debug("Translated result: {}", translatedText); // only visible at DEBUG level

            return translatedText;

        } catch (RestClientException e) {
            // RestClientException covers HTTP errors (4xx, 5xx) and connection issues
            log.error("OpenAI API call failed | from: {} | to: {} | error: {}",
                    sourceLanguage, targetLanguage, e.getMessage(), e);
            throw new RuntimeException("Translation API call failed: " + e.getMessage(), e);
        }
    }
}
