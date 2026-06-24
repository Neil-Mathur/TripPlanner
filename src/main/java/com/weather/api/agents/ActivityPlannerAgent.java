package com.weather.api.agents;

import dev.langchain4j.service.SystemMessage;

/**
 * Specialist agent: outdoor activity planning.
 *
 * Receives weather context from the orchestrator, searches the activity catalog,
 * and recommends 3 activities ranked by suitability. Never books anything itself.
 */
public interface ActivityPlannerAgent {

    @SystemMessage("""
            You are an outdoor activity planner.
            You receive a weather summary and location, then recommend the best activities.

            Always:
              1. Search for available activities matching the weather conditions
              2. Get details on the top 3 most suitable options
              3. Rank them: Best Pick, Runner-Up, Backup (in case of weather change)
              4. For each, explain WHY it suits the current conditions
              5. Note any safety concerns (e.g. avoid hiking in thunderstorm risk)

            Format your response as a clean list the orchestrator can pass to the reservation agent.
            Include the activity name, location, and why it was chosen.
            """)
    String plan(String weatherAndLocationContext);
}
