package com.weather.api.config;

import com.weather.api.agents.ActivityPlannerAgent;
import com.weather.api.agents.ReservationAgent;
import com.weather.api.agents.TripOrchestratorAgent;
import com.weather.api.agents.WeatherAgent;
import com.weather.api.orchestrator.OrchestratorTools;
import com.weather.api.tools.ActivityTools;
import com.weather.api.tools.ReservationTools;
import com.weather.api.tools.WeatherTools;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires up the multi-agent system.
 *
 * Architecture:
 *
 *   TripOrchestratorAgent
 *         │ tools: OrchestratorTools
 *         │
 *         ├─── askWeatherAgent    ──▶  WeatherAgent
 *         │                               tools: WeatherTools (wttr.in)
 *         │
 *         ├─── askActivityPlanner ──▶  ActivityPlannerAgent
 *         │                               tools: ActivityTools (mock catalog)
 *         │
 *         └─── askReservationAgent ──▶ ReservationAgent
 *                                          tools: ReservationTools (mock booking)
 *
 * All agents share the same ChatLanguageModel bean (defined in AgentConfig).
 * In a production system you might give each agent a different model or temperature.
 */
@Configuration
public class MultiAgentConfig {

    @Bean
    public WeatherAgent weatherAgent(ChatLanguageModel model, WeatherTools weatherTools) {
        return AiServices.builder(WeatherAgent.class)
                .chatLanguageModel(model)
                .tools(weatherTools)
                .build();
    }

    @Bean
    public ActivityPlannerAgent activityPlannerAgent(ChatLanguageModel model, ActivityTools activityTools) {
        return AiServices.builder(ActivityPlannerAgent.class)
                .chatLanguageModel(model)
                .tools(activityTools)
                .build();
    }

    @Bean
    public ReservationAgent reservationAgent(ChatLanguageModel model, ReservationTools reservationTools) {
        return AiServices.builder(ReservationAgent.class)
                .chatLanguageModel(model)
                .tools(reservationTools)
                .build();
    }

    @Bean
    public TripOrchestratorAgent tripOrchestratorAgent(ChatLanguageModel model,
                                                        OrchestratorTools orchestratorTools) {
        return AiServices.builder(TripOrchestratorAgent.class)
                .chatLanguageModel(model)
                .tools(orchestratorTools)
                .build();
    }
}
