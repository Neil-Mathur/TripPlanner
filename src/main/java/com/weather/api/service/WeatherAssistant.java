package com.weather.api.service;

import dev.langchain4j.service.SystemMessage;

/**
 * The agent interface.
 *
 * LangChain4j's AiServices reads this interface and generates an implementation
 * at runtime that: sends the question to the LLM, runs the ReAct tool-calling
 * loop (equivalent to LangGraph's create_react_agent), and returns the final answer.
 *
 * The @SystemMessage sets the LLM's persona — equivalent to the SYSTEM_PROMPT
 * string in the Python agent.
 */
public interface WeatherAssistant {

    @SystemMessage("""
            You are a helpful weather assistant.
            Use the available tools to provide accurate, friendly weather information.
            When asked about multiple locations, look them all up.
            """)
    String ask(String question);
}
