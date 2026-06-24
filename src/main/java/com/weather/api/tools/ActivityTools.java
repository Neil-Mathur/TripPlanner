package com.weather.api.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Mock activity catalog. In a real system these would call an activities API
 * (e.g. Viator, GetYourGuide, Airbnb Experiences).
 */
@Component
public class ActivityTools {

    private static final Map<String, List<String>> BY_CONDITION = Map.of(
            "sunny",  List.of("Kayaking", "Hiking", "Cycling Tour", "Open-Air Yoga", "Rock Climbing"),
            "cloudy", List.of("Nature Photography Walk", "Botanical Garden Tour", "Mountain Biking", "Frisbee Golf"),
            "rainy",  List.of("Indoor Rock Climbing", "Pottery Class", "Escape Room", "Cooking Workshop"),
            "windy",  List.of("Kitesurfing", "Sailing", "Paragliding", "Wind Surfing")
    );

    private static final Map<String, List<String>> BY_CATEGORY = Map.of(
            "adventure",   List.of("Rock Climbing", "Kayaking", "Paragliding", "Kitesurfing", "Hiking"),
            "relaxation",  List.of("Open-Air Yoga", "Botanical Garden Tour", "Nature Photography Walk"),
            "culture",     List.of("Cooking Workshop", "Pottery Class", "Historic Walking Tour"),
            "sports",      List.of("Cycling Tour", "Mountain Biking", "Frisbee Golf", "Sailing")
    );

    @Tool("Search the activity catalog for options matching a weather condition and category. " +
          "Returns a list of available activities at the location.")
    public String searchActivities(
            @P("City or location name") String location,
            @P("Weather condition: sunny, cloudy, rainy, or windy") String weatherCondition,
            @P("Activity category: adventure, relaxation, culture, or sports") String category) {

        List<String> byWeather = BY_CONDITION.getOrDefault(
                weatherCondition.toLowerCase(), BY_CONDITION.get("cloudy"));
        List<String> byCategory = BY_CATEGORY.getOrDefault(
                category.toLowerCase(), BY_CATEGORY.get("adventure"));

        // Intersection first, then fill from weather list
        List<String> combined = byWeather.stream()
                .filter(byCategory::contains)
                .toList();
        List<String> result = combined.isEmpty() ? byWeather : combined;

        StringBuilder sb = new StringBuilder(
                String.format("Activities in %s for %s/%s conditions:%n", location, weatherCondition, category));
        result.forEach(a -> sb.append("  • ").append(a).append("\n"));
        sb.append("\nAll activities accommodate 1–10 people. Weekend booking recommended 48 h in advance.");
        return sb.toString();
    }

    @Tool("Get full details for a specific activity: duration, cost, difficulty, meeting point, and cancellation policy.")
    public String getActivityDetails(
            @P("Name of the activity, e.g. 'Kayaking'") String activityName,
            @P("City or location") String location) {

        // Mock detail data — keyed roughly by activity type
        String difficulty = activityName.contains("Climbing") || activityName.contains("Paragliding")
                ? "Challenging" : "Moderate";
        String cost = activityName.contains("Cooking") || activityName.contains("Pottery")
                ? "$60–80 per person" : "$30–55 per person";
        String duration = activityName.contains("Tour") ? "4–5 hours" : "2–3 hours";

        return String.format("""
                %s — %s
                  Duration          : %s
                  Cost              : %s
                  Difficulty        : %s
                  Minimum Age       : 14 years
                  Equipment         : Provided on-site (bring water and sunscreen)
                  Meeting Point     : %s Adventure Hub, Main Square
                  Cancellation      : Full refund up to 24 h before start
                  Group Discount    : 10%% off for 6+ people
                """,
                activityName, location,
                duration, cost, difficulty, location);
    }
}
