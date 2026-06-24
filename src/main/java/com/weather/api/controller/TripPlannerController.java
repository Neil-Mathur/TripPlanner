package com.weather.api.controller;

import com.weather.api.agents.TripOrchestratorAgent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trip")
public class TripPlannerController {

    private final TripOrchestratorAgent orchestrator;

    public TripPlannerController(TripOrchestratorAgent orchestrator) {
        this.orchestrator = orchestrator;
    }

    record PlanRequest(String request) {}
    record PlanResponse(String plan) {}

    /**
     * POST /api/trip/plan
     *
     * Body:  { "request": "Plan a weekend trip to Chicago on 2026-07-05 for 3 people who enjoy adventure" }
     * Reply: { "plan": "=== WEEKEND TRIP PLAN === ..." }
     *
     * Internally orchestrates: WeatherAgent → ActivityPlannerAgent → ReservationAgent
     */
    @PostMapping("/plan")
    public ResponseEntity<PlanResponse> plan(@RequestBody PlanRequest request) {
        if (request.request() == null || request.request().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        String plan = orchestrator.plan(request.request());
        return ResponseEntity.ok(new PlanResponse(plan));
    }
}
