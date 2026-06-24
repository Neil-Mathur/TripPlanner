package com.weather.api.controller;

import com.weather.api.service.WeatherAssistant;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/weather")
public class WeatherController {

    private final WeatherAssistant weatherAssistant;

    public WeatherController(WeatherAssistant weatherAssistant) {
        this.weatherAssistant = weatherAssistant;
    }

    record AskRequest(String question) {}
    record AskResponse(String answer) {}

    /**
     * POST /api/weather/ask
     *
     * Body:  { "question": "What's the weather in Tokyo?" }
     * Reply: { "answer": "The weather in Tokyo is currently..." }
     */
    @PostMapping("/ask")
    public ResponseEntity<AskResponse> ask(@RequestBody AskRequest request) {
        if (request.question() == null || request.question().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        String answer = weatherAssistant.ask(request.question());
        return ResponseEntity.ok(new AskResponse(answer));
    }
}
