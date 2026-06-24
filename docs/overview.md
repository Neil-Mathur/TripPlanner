# Weather Spring API — Overview

## What This Project Is

A Spring Boot REST API that exposes the same weather agent capability as the Python project, but implemented entirely in Java using **LangChain4j** as the AI framework.

Send it a question in plain English. It calls OpenAI, which decides which weather tool(s) to invoke, calls `wttr.in` for live data, and returns a natural-language answer.

```
POST /api/weather/ask
{ "question": "What's the weather in Lucknow, India?" }

→ { "answer": "The weather in Lucknow, India is currently hazy with a temperature of 38°C..." }
```

---

## Technology Mapping: Python → Java

| Python project | Java project | Role |
|---|---|---|
| `langchain` (`@tool`) | `langchain4j` (`@Tool`) | Defines tools the LLM can call |
| `langgraph` (`create_react_agent`) | `langchain4j` (`AiServices`) | Runs the agent loop |
| `langchain-openai` (`ChatOpenAI`) | `langchain4j-open-ai` (`OpenAiChatModel`) | Connects to OpenAI's API |
| `FastAPI` / none | `spring-boot-starter-web` | Exposes the REST endpoint |
| `agent.py` `ask()` | `WeatherAssistant.ask()` | Public API for asking questions |

> **Note:** In Java, LangChain4j covers what both LangChain and LangGraph do in Python. There is no separate "LangGraph for Java" library in mainstream use — `AiServices` implements the same ReAct agent loop internally.

---

## Project Structure

```
WeatherSpringApi/
├── pom.xml
├── src/main/java/com/weather/api/
│   ├── WeatherApiApplication.java     ← Spring Boot entry point
│   ├── config/
│   │   └── AgentConfig.java           ← wires LLM + tools + agent (≈ agent.py setup)
│   ├── service/
│   │   └── WeatherAssistant.java      ← agent interface with @SystemMessage
│   ├── tools/
│   │   └── WeatherTools.java          ← @Tool methods (≈ tools/weather.py)
│   └── controller/
│       └── WeatherController.java     ← REST endpoint POST /api/weather/ask
└── src/main/resources/
    └── application.properties
```

---

## Quick Start

**Prerequisites:** Java 21, Maven 3.9+, an OpenAI API key with credits.

```powershell
# Set your key
$env:OPENAI_API_KEY = "sk-proj-..."

# Build and run
cd D:\NeilProjects\JavaAgents\WeatherSpringApi
mvn spring-boot:run
```

**Test it:**
```powershell
curl -X POST http://localhost:8080/api/weather/ask `
  -H "Content-Type: application/json" `
  -d '{"question": "What is the weather in Delhi right now?"}'
```

---

## Further Reading

- [langchain4j.md](langchain4j.md) — How LangChain4j maps to Python's LangChain + LangGraph
- [agent-flow.md](agent-flow.md) — Step-by-step walkthrough of what happens on each request
- [extending.md](extending.md) — How to add new tools
