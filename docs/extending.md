# Extending the Agent

Adding a new capability requires the same three steps as the Python project.

---

## Adding a New Tool

### Step 1 — Add a method to `WeatherTools` (or a new `@Component` class)

```java
@Tool("Get the UV index and sun safety advice for a location.")
public String getUvIndex(
        @P("City name or location") String location) {
    // call an API, return a formatted string
    return "UV Index in " + location + ": 8 (Very High). Wear SPF 50+.";
}
```

Rules:
- Write a specific `@Tool("...")` description — the LLM reads this to decide when to call it
- Use `@P("...")` on each parameter — the LLM uses these to know what values to pass
- Return a plain `String` — the LLM reads the return value directly
- Catch exceptions and return error strings instead of letting them propagate

### Step 2 — Register the new class as a Spring bean (if in a new file)

```java
@Component          // already handled if in the same WeatherTools class
public class UvTools { ... }
```

### Step 3 — Add it to `AgentConfig`

If you added the method to the existing `WeatherTools` class, no change is needed — Spring injects the whole object and LangChain4j scans all `@Tool` methods automatically.

If you created a new `@Component` class:

```java
@Bean
public WeatherAssistant weatherAssistant(
        ChatLanguageModel model,
        WeatherTools weatherTools,
        UvTools uvTools) {           // inject the new tool class
    return AiServices.builder(WeatherAssistant.class)
            .chatLanguageModel(model)
            .tools(weatherTools, uvTools)   // add it here
            .build();
}
```

---

## Changing the Model

In `AgentConfig.java`:

```java
OpenAiChatModel.builder()
    .apiKey(openAiApiKey)
    .modelName("gpt-4o")        // change from gpt-4o-mini to gpt-4o
    .timeout(Duration.ofSeconds(60))
    .build();
```

---

## Switching to a Different LLM Provider

LangChain4j supports many providers through the same `ChatLanguageModel` interface.

**Anthropic Claude:**
```xml
<!-- pom.xml -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-anthropic</artifactId>
    <version>${langchain4j.version}</version>
</dependency>
```
```java
// AgentConfig.java
import dev.langchain4j.model.anthropic.AnthropicChatModel;

@Bean
public ChatLanguageModel chatLanguageModel() {
    return AnthropicChatModel.builder()
            .apiKey(System.getenv("ANTHROPIC_API_KEY"))
            .modelName("claude-opus-4-8")
            .build();
}
```

**Google Gemini:**
```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-google-ai-gemini</artifactId>
    <version>${langchain4j.version}</version>
</dependency>
```
```java
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;

@Bean
public ChatLanguageModel chatLanguageModel() {
    return GoogleAiGeminiChatModel.builder()
            .apiKey(System.getenv("GOOGLE_API_KEY"))
            .modelName("gemini-1.5-pro")
            .build();
}
```

Only `AgentConfig.java` changes. The tools, controller, and assistant interface are provider-agnostic.

---

## Adding a New REST Endpoint

To expose the agent differently (e.g., a `GET /api/weather/current?location=Delhi`):

```java
@GetMapping("/current")
public ResponseEntity<AskResponse> current(@RequestParam String location) {
    String answer = weatherAssistant.ask("What is the current weather in " + location + "?");
    return ResponseEntity.ok(new AskResponse(answer));
}
```

Add this to `WeatherController`. No changes to the agent or tools needed.
