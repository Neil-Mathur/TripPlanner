# Multi-Agent Architecture

## The Problem Single Agents Have

A single agent with many tools works fine for simple questions. But when a task has distinct concerns — check weather, then plan activities, then book a venue — a single agent tends to:

- Mix responsibilities in one system prompt, making it harder to tune
- Lose context as the conversation grows longer
- Become hard to debug ("which part of the logic went wrong?")

Multi-agent systems solve this by giving each concern its own specialist agent, each with its own prompt, tools, and context window.

---

## The Pattern Used: Agent-as-Tool

The approach here is **Agent-as-Tool**: child agents are wrapped in `@Tool` methods and given to the orchestrator. From the orchestrator's perspective, calling a child agent looks identical to calling a weather API — it sends some text and receives some text back.

```
TripOrchestratorAgent (LLM)
    │
    │  sees three "tools":
    │
    ├── askWeatherAgent("Chicago weather this weekend")
    │       └── runs WeatherAgent (full LLM loop with WeatherTools)
    │             └── calls wttr.in, returns weather summary
    │
    ├── askActivityPlanner("sunny, 28°C, Chicago, adventure, 3 people")
    │       └── runs ActivityPlannerAgent (full LLM loop with ActivityTools)
    │             └── searches mock catalog, returns ranked activities
    │
    └── askReservationAgent("Kayaking, Chicago, 2026-07-05, 3 people, Neil")
            └── runs ReservationAgent (full LLM loop with ReservationTools)
                  └── checks availability, books, returns confirmation
```

Each child agent runs its own complete ReAct loop. The orchestrator does not know or care what happens inside them.

---

## How It Works in Code

### 1 — Child agents are plain `AiServices` interfaces

```java
public interface WeatherAgent {
    @SystemMessage("You are a specialist weather agent...")
    String answer(String question);
}

// Built in MultiAgentConfig.java
WeatherAgent weatherAgent = AiServices.builder(WeatherAgent.class)
        .chatLanguageModel(model)
        .tools(weatherTools)
        .build();
```

A child agent has its own system prompt, its own tools, and its own scope. It knows nothing about the orchestrator or the other agents.

### 2 — Child agents are wrapped as @Tool methods

```java
@Component
public class OrchestratorTools {

    private final WeatherAgent weatherAgent;
    // ...

    @Tool("Delegate to the Weather Agent to get current conditions...")
    public String askWeatherAgent(String question) {
        return weatherAgent.answer(question);   // <-- child agent runs here
    }
}
```

The child agent's `answer()` call triggers a full LLM round trip with all its own tools. From the outside it just returns a String.

### 3 — The orchestrator uses OrchestratorTools

```java
public interface TripOrchestratorAgent {
    @SystemMessage("You are a trip planning orchestrator. ALWAYS: 1) ask weather, 2) plan activities, 3) book...")
    String plan(String userRequest);
}

// In MultiAgentConfig.java
TripOrchestratorAgent orchestrator = AiServices.builder(TripOrchestratorAgent.class)
        .chatLanguageModel(model)
        .tools(orchestratorTools)   // <-- the wrapped child agents
        .build();
```

The orchestrator LLM decides when to call which child agent, in what order, and what to pass. The system prompt guides the sequence.

---

## File Map

```
agents/
├── WeatherAgent.java           ← specialist: weather only
├── ActivityPlannerAgent.java   ← specialist: activity suggestions
├── ReservationAgent.java       ← specialist: check & book venues
└── TripOrchestratorAgent.java  ← coordinator: delegates to the above

orchestrator/
└── OrchestratorTools.java      ← wraps each child agent as a @Tool

tools/
├── WeatherTools.java           ← WeatherAgent's tools (live wttr.in API)
├── ActivityTools.java          ← ActivityPlannerAgent's tools (mock catalog)
└── ReservationTools.java       ← ReservationAgent's tools (mock booking)

config/
├── AgentConfig.java            ← ChatLanguageModel + single WeatherAssistant
└── MultiAgentConfig.java       ← wires all 4 multi-agent beans

controller/
├── WeatherController.java      ← POST /api/weather/ask (single agent)
└── TripPlannerController.java  ← POST /api/trip/plan  (multi-agent)
```

---

## Sequence for "Plan a trip to Chicago"

```
User: "Plan a trip to Chicago on 2026-07-05 for 3 people who enjoy adventure"
       │
       ▼
TripOrchestratorAgent
  LLM thinks: "I need weather first."
  Calls: askWeatherAgent("Current weather and 3-day forecast for Chicago")
       │
       ▼ (child agent loop begins)
  WeatherAgent
    LLM thinks: "I'll call get_current_weather and get_weather_forecast"
    Tool: get_current_weather("Chicago")     → wttr.in → "Sunny, 28°C..."
    Tool: get_weather_forecast("Chicago", 3) → wttr.in → "Sat: sunny, Sun: cloudy..."
    LLM synthesises → returns weather summary string
       │
       ▼ (back in orchestrator loop)
  OrchestratorAgent receives weather summary
  LLM thinks: "Now I'll plan activities."
  Calls: askActivityPlanner("Chicago, sunny 28°C, 2026-07-05, 3 people, adventure")
       │
       ▼ (child agent loop begins)
  ActivityPlannerAgent
    LLM thinks: "Search for adventure activities in sunny weather"
    Tool: searchActivities("Chicago", "sunny", "adventure") → Kayaking, Rock Climbing, ...
    Tool: getActivityDetails("Kayaking", "Chicago")         → duration, cost, meeting point
    LLM ranks and explains → returns "Top Pick: Kayaking..."
       │
       ▼ (back in orchestrator loop)
  OrchestratorAgent receives activity list
  LLM thinks: "Now book the top pick."
  Calls: askReservationAgent("Kayaking, Chicago, 2026-07-05, 3 people, Neil Mathur")
       │
       ▼ (child agent loop begins)
  ReservationAgent
    Tool: checkAvailability("Kayaking", "2026-07-05", 3)       → available, slots: 9AM/2PM
    Tool: makeReservation("Kayaking", "2026-07-05", "2:00 PM", 3, "Neil Mathur") → WA-042891
    LLM confirms → returns booking confirmation
       │
       ▼ (back in orchestrator loop)
  OrchestratorAgent synthesises all three responses
  Returns: === WEEKEND TRIP PLAN === ...
       │
       ▼
TripPlannerController → { "plan": "..." }
```

---

## Why Each Agent Has Its Own System Prompt

Each system prompt is tuned for the agent's specific job:

| Agent | Prompt focus |
|---|---|
| `WeatherAgent` | "Be direct. No filler. Give clean data." — structured output for downstream use |
| `ActivityPlannerAgent` | "Explain WHY each activity suits conditions." — reasoning, safety notes |
| `ReservationAgent` | "Always confirm booking details." — reliability, fallback on full bookings |
| `TripOrchestratorAgent` | "ALWAYS follow this sequence." — coordination discipline |

The orchestrator's prompt explicitly tells the LLM the order to call agents. Without this, the LLM might skip steps or try to guess activities without checking weather first.

---

## Key Design Decisions

**Why not one big agent with all tools?**
Six tools (2 weather + 2 activity + 2 reservation) in one agent would work for simple cases. But the system prompt would become a wall of instructions. Debugging "why did it skip the reservation?" becomes hard. Separate agents give you clean boundaries.

**Why share the same ChatLanguageModel?**
Cost and simplicity. In production you might give the orchestrator a more capable (expensive) model since it's doing more reasoning, and give tool-calling child agents a faster/cheaper model.

**Why mock data for activity and reservation tools?**
So the project runs without external API keys or paid services. In a real system, `ActivityTools` would call Viator or GetYourGuide, and `ReservationTools` would call Fareharbor or a venue's booking API.
