# Agent Flow — Step by Step

What happens when you call `POST /api/weather/ask` with `"What's the weather in Delhi?"`.

---

## Request Flow

```
HTTP POST /api/weather/ask
{ "question": "What's the weather in Delhi?" }
         │
         ▼
WeatherController.ask()
  - validates the request body
  - calls weatherAssistant.ask("What's the weather in Delhi?")
         │
         ▼
LangChain4j AiServices (the agent loop)
  │
  ├─ Step 1: Build the prompt
  │    SystemMessage: "You are a helpful weather assistant..."
  │    UserMessage:   "What's the weather in Delhi?"
  │    Tool schemas:  getCurrentWeather(location), getWeatherForecast(location, days)
  │
  ├─ Step 2: Call OpenAI (GPT-4o-mini)
  │    OpenAI reads the question and available tools.
  │    Response: tool_call → getCurrentWeather("Delhi")
  │
  ├─ Step 3: Execute the tool
  │    WeatherTools.getCurrentWeather("Delhi") is called
  │    → HTTP GET https://wttr.in/Delhi?format=j1
  │    → Jackson parses the JSON response
  │    → returns "Weather in Delhi, India: Condition: Haze, Temp: 40°C..."
  │
  ├─ Step 4: Send tool result back to OpenAI
  │    Conversation now contains:
  │      SystemMessage, UserMessage, AIMessage(tool_call), ToolResultMessage
  │    OpenAI reads the tool result and decides: enough info, write final answer.
  │
  └─ Step 5: OpenAI returns final answer
       "The weather in Delhi, India is currently hazy with a temperature of 40°C..."
         │
         ▼
WeatherController returns:
{ "answer": "The weather in Delhi, India is currently hazy..." }
```

---

## How Multi-Location Questions Work

If the user asks: `"Compare weather in Delhi and Mumbai"`, OpenAI will call `getCurrentWeather` **twice** — once per city — before composing the final answer. LangChain4j's loop handles this automatically.

```
UserMessage: "Compare weather in Delhi and Mumbai"
  │
  ├─ Tool call 1: getCurrentWeather("Delhi")   → result 1
  ├─ Tool call 2: getCurrentWeather("Mumbai")  → result 2
  └─ Final answer: "Delhi is 40°C and hazy. Mumbai is 32°C with..."
```

---

## Class Responsibilities

| Class | Responsibility |
|---|---|
| `WeatherController` | HTTP layer — receives JSON request, returns JSON response |
| `WeatherAssistant` | Agent interface — defines the contract (`ask(String) → String`) and system prompt |
| `AgentConfig` | Wires LLM + tools + agent at startup; creates the `WeatherAssistant` Spring bean |
| `WeatherTools` | Tool implementations — calls `wttr.in`, parses JSON, returns formatted strings |
| `OpenAiChatModel` | LangChain4j's OpenAI wrapper — sends prompts, receives responses |

---

## Sequence Diagram

```
Client         Controller      AiServices         OpenAI          wttr.in
  │                │                │                │               │
  │  POST /ask     │                │                │               │
  │──────────────▶│                │                │               │
  │               │  assistant.ask()│                │               │
  │               │───────────────▶│                │               │
  │               │                │  chat(messages)│               │
  │               │                │───────────────▶│               │
  │               │                │  tool_call     │               │
  │               │                │◀───────────────│               │
  │               │                │  GET /Delhi    │               │
  │               │                │───────────────────────────────▶│
  │               │                │  weather JSON  │               │
  │               │                │◀───────────────────────────────│
  │               │                │  chat(result)  │               │
  │               │                │───────────────▶│               │
  │               │                │  final answer  │               │
  │               │                │◀───────────────│               │
  │               │  answer String │                │               │
  │               │◀───────────────│                │               │
  │  { answer }   │                │                │               │
  │◀──────────────│                │                │               │
```
