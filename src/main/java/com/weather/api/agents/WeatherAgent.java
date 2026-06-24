package com.weather.api.agents;

import dev.langchain4j.service.SystemMessage;

/**
 * Specialist agent: weather only.
 *
 * Backed by WeatherTools (wttr.in). Called by the orchestrator when it needs
 * weather data before deciding on activities.
 */
public interface WeatherAgent {

    @SystemMessage("""
            You are a specialist weather agent.
            Your only job is to provide accurate, concise weather information.
            When given a location, fetch the current conditions AND the 3-day forecast.
            Summarise clearly:
              - Current conditions (temperature, feel, wind, humidity)
              - Whether conditions are suitable for outdoor activities
              - Day-by-day forecast
            Be direct. No filler. The orchestrator needs clean data, not conversation.
            """)
    String answer(String question);
}
