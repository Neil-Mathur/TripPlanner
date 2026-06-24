package com.weather.api.config;

import com.weather.api.service.WeatherAssistant;
import com.weather.api.tools.WeatherTools;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Wires together the LLM, tools, and agent — the Java equivalent of
 * the setup section in Python's agent.py.
 *
 * LangChain4j's AiServices.builder() implements the same ReAct agent loop
 * that LangGraph's create_react_agent provides in Python:
 *   1. Send question + tool schemas to the LLM
 *   2. If LLM calls a tool → execute it, send result back to LLM
 *   3. Repeat until LLM produces a plain-text answer
 */
@Configuration
public class AgentConfig {

    @Value("${openai.api.key}")
    private String openAiApiKey;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return OpenAiChatModel.builder()
                .apiKey(openAiApiKey)
                .modelName("gpt-4o-mini")
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    @Bean
    public WeatherAssistant weatherAssistant(ChatLanguageModel model, WeatherTools tools) {
        return AiServices.builder(WeatherAssistant.class)
                .chatLanguageModel(model)
                .tools(tools)
                .build();
    }
}
