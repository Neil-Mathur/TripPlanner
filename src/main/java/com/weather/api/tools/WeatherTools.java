package com.weather.api.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Weather tools powered by wttr.in (no API key required).
 *
 * Each method annotated with @Tool is visible to the LLM. The annotation's
 * description text is what the model reads to decide when to call the tool —
 * equivalent to the docstring on a Python @tool function.
 */
@Component
public class WeatherTools {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    @Tool("Get current weather conditions for a city or location. " +
          "Use this when the user asks about current or today's weather.")
    public String getCurrentWeather(
            @P("City name or location, e.g. 'London' or 'New York, NY'") String location) {
        try {
            JsonNode data = fetchWttrJson(location);

            JsonNode current = data.get("current_condition").get(0);
            JsonNode area   = data.get("nearest_area").get(0);
            String areaName = area.get("areaName").get(0).get("value").asText();
            String country  = area.get("country").get(0).get("value").asText();

            return String.format("""
                    Weather in %s, %s:
                      Condition : %s
                      Temp      : %s°C / %s°F
                      Feels like: %s°C
                      Humidity  : %s%%
                      Wind      : %s km/h %s
                      Visibility: %s km""",
                    areaName, country,
                    current.get("weatherDesc").get(0).get("value").asText(),
                    current.get("temp_C").asText(),
                    current.get("temp_F").asText(),
                    current.get("FeelsLikeC").asText(),
                    current.get("humidity").asText(),
                    current.get("windspeedKmph").asText(),
                    current.get("winddir16Point").asText(),
                    current.get("visibility").asText());

        } catch (Exception e) {
            return "Error fetching weather for '" + location + "': " + e.getMessage();
        }
    }

    @Tool("Get a multi-day weather forecast (1 to 3 days) for a city or location. " +
          "Use this when the user asks about future weather or a forecast.")
    public String getWeatherForecast(
            @P("City name or location, e.g. 'Tokyo'") String location,
            @P("Number of forecast days, between 1 and 3") int days) {
        days = Math.max(1, Math.min(days, 3));
        try {
            JsonNode data = fetchWttrJson(location);

            JsonNode area   = data.get("nearest_area").get(0);
            String areaName = area.get("areaName").get(0).get("value").asText();
            String country  = area.get("country").get(0).get("value").asText();

            StringBuilder sb = new StringBuilder("Forecast for ")
                    .append(areaName).append(", ").append(country).append(":\n");

            JsonNode weather = data.get("weather");
            for (int i = 0; i < Math.min(days, weather.size()); i++) {
                JsonNode day  = weather.get(i);
                String date   = day.get("date").asText();
                String desc   = day.get("hourly").get(4).get("weatherDesc").get(0).get("value").asText();
                String minC   = day.get("mintempC").asText();
                String maxC   = day.get("maxtempC").asText();
                String minF   = day.get("mintempF").asText();
                String maxF   = day.get("maxtempF").asText();
                sb.append(String.format("  %s: %s, %s–%s°C / %s–%s°F%n",
                        date, desc, minC, maxC, minF, maxF));
            }
            return sb.toString().trim();

        } catch (Exception e) {
            return "Error fetching forecast for '" + location + "': " + e.getMessage();
        }
    }

    private JsonNode fetchWttrJson(String location) throws Exception {
        String encoded = URLEncoder.encode(location, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://wttr.in/" + encoded + "?format=j1"))
                .header("User-Agent", "weather-agent/1.0")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return mapper.readTree(response.body());
    }
}
