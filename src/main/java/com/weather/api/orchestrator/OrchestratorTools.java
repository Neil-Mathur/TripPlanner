package com.weather.api.orchestrator;

import com.weather.api.agents.ActivityPlannerAgent;
import com.weather.api.agents.ReservationAgent;
import com.weather.api.agents.WeatherAgent;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

/**
 * The orchestrator's toolbox.
 *
 * Each method here wraps a child agent as a @Tool. This is the core of the
 * Agent-as-Tool multi-agent pattern:
 *
 *   OrchestratorAgent (LLM) sees three tools:
 *     askWeatherAgent    → internally calls WeatherAgent (another LLM loop)
 *     askActivityPlanner → internally calls ActivityPlannerAgent (another LLM loop)
 *     askReservationAgent → internally calls ReservationAgent (another LLM loop)
 *
 * Each child agent runs its own full ReAct loop (with its own tools) and returns
 * a plain String. From the orchestrator's perspective, it just called a function.
 *
 * Equivalent to Python LangGraph's subgraph pattern.
 */
@Component
public class OrchestratorTools {

    private final WeatherAgent weatherAgent;
    private final ActivityPlannerAgent activityPlannerAgent;
    private final ReservationAgent reservationAgent;

    public OrchestratorTools(WeatherAgent weatherAgent,
                             ActivityPlannerAgent activityPlannerAgent,
                             ReservationAgent reservationAgent) {
        this.weatherAgent = weatherAgent;
        this.activityPlannerAgent = activityPlannerAgent;
        this.reservationAgent = reservationAgent;
    }

    @Tool("Delegate to the Weather Agent to get current conditions and forecast for a location. " +
          "Always call this FIRST before planning activities.")
    public String askWeatherAgent(
            @P("Full weather question including location and whether current or forecast is needed, " +
               "e.g. 'Current weather and 3-day forecast for Chicago'")
            String question) {
        return weatherAgent.answer(question);
    }

    @Tool("Delegate to the Activity Planner Agent to suggest suitable outdoor activities. " +
          "Pass the full weather summary and location so it can make appropriate recommendations.")
    public String askActivityPlanner(
            @P("Context string containing: location, weather conditions, date, party size, " +
               "and any user preferences for activity type (adventure, relaxation, culture, sports)")
            String context) {
        return activityPlannerAgent.plan(context);
    }

    @Tool("Delegate to the Reservation Agent to check availability and book a venue. " +
          "Pass the recommended activities list, location, date, party size, and booking name.")
    public String askReservationAgent(
            @P("Reservation request containing: activity recommendations, location, " +
               "date (YYYY-MM-DD), party size, and the name for the booking")
            String request) {
        return reservationAgent.reserve(request);
    }
}
