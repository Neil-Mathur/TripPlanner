# LangChain4j — Java Equivalent of LangChain + LangGraph

## What LangChain4j Is

LangChain4j is the Java port of the LangChain ecosystem. A single library covers what Python splits across two packages:

| Python | Java (LangChain4j) |
|---|---|
| `langchain` — `@tool` decorator, message types | `langchain4j` core — `@Tool`, `AiMessage`, etc. |
| `langgraph` — `create_react_agent`, agent loop | `langchain4j` core — `AiServices`, ReAct loop |
| `langchain-openai` — `ChatOpenAI` | `langchain4j-open-ai` — `OpenAiChatModel` |

There is no separate "LangGraph4j" that you need to depend on. The agent loop is built into `AiServices`.

---

## The @Tool Annotation

`@Tool` in Java does exactly what `@tool` does in Python: it packages a method's name, description, and parameter types into a schema the LLM can read.

**Python:**
```python
from langchain_core.tools import tool

@tool
def get_current_weather(location: str) -> str:
    """Get current weather conditions for a city or location.
    Args:
        location: City name or location, e.g. "London"
    """
    ...
```

**Java equivalent:**
```java
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;

@Tool("Get current weather conditions for a city or location.")
public String getCurrentWeather(
        @P("City name or location, e.g. 'London'") String location) {
    ...
}
```

Key differences:
- The description goes directly in `@Tool("...")` rather than the docstring body
- Parameter descriptions use `@P("...")` instead of the `Args:` section of the docstring
- The method lives in a plain Spring `@Component` class; no special base class needed

---

## AiServices — The Agent Loop

`AiServices` is LangChain4j's equivalent of LangGraph's `create_react_agent`. It implements the same ReAct loop:

**Python (LangGraph):**
```python
from langgraph.prebuilt import create_react_agent

agent = create_react_agent(llm, tools, prompt=SYSTEM_PROMPT)
result = agent.invoke({"messages": [HumanMessage(content=question)]})
answer = result["messages"][-1].content
```

**Java (LangChain4j):**
```java
// Define the interface
public interface WeatherAssistant {
    @SystemMessage("You are a helpful weather assistant...")
    String ask(String question);
}

// Build the agent (done once at startup via Spring @Bean)
WeatherAssistant assistant = AiServices.builder(WeatherAssistant.class)
        .chatLanguageModel(model)
        .tools(weatherTools)
        .build();

// Call it
String answer = assistant.ask("What's the weather in Tokyo?");
```

The generated implementation:
1. Sends the question + tool schemas to OpenAI
2. If OpenAI returns a tool call → invokes the `@Tool` method
3. Sends the tool result back to OpenAI
4. Repeats until OpenAI returns a plain-text answer
5. Returns that answer as a `String`

The caller sees a single method call returning a String. The multi-step loop is invisible.

---

## The System Prompt

**Python:**
```python
SYSTEM_PROMPT = "You are a helpful weather assistant..."
agent = create_react_agent(llm, tools, prompt=SYSTEM_PROMPT)
```

**Java:**
```java
public interface WeatherAssistant {
    @SystemMessage("You are a helpful weather assistant...")
    String ask(String question);
}
```

The `@SystemMessage` annotation on the interface method is equivalent to passing a system prompt string to `create_react_agent`. LangChain4j injects it as the first message in every conversation.

---

## OpenAiChatModel vs ChatOpenAI

**Python:**
```python
from langchain_openai import ChatOpenAI
llm = ChatOpenAI(model="gpt-4o-mini")
```

**Java:**
```java
import dev.langchain4j.model.openai.OpenAiChatModel;

ChatLanguageModel model = OpenAiChatModel.builder()
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .modelName("gpt-4o-mini")
        .timeout(Duration.ofSeconds(60))
        .build();
```

Both wrap OpenAI's chat completions API. The Java version requires explicit timeout configuration (Python's `requests` defaults are more lenient).

---

## What LangChain4j Does NOT Provide (vs. LangGraph)

LangChain4j's `AiServices` covers the common "single agent with tools" use case perfectly. LangGraph in Python goes further with:

- **Graph-based flows** — multiple agents or nodes with conditional edges
- **Checkpointing** — save and resume agent state mid-run
- **Streaming** — emit partial results as the agent works

LangChain4j does support streaming via `StreamingChatLanguageModel`, but multi-agent graph flows equivalent to full LangGraph are not built-in. For those cases in Java, the community `langgraph4j` library (by bsorrentino on GitHub) is the closest equivalent, though it is not as widely adopted.
