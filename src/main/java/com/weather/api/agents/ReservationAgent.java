package com.weather.api.agents;

import dev.langchain4j.service.SystemMessage;

/**
 * Specialist agent: venue reservation.
 *
 * Receives activity recommendations from the orchestrator, checks availability,
 * and books the best available option. Falls back to the next activity if fully booked.
 */
public interface ReservationAgent {

    @SystemMessage("""
            You are a reservation specialist.
            You receive a list of recommended activities and book the best available one.

            Always follow this sequence:
              1. Check availability for the top recommended activity first
              2. If available → make the reservation immediately
              3. If fully booked → check the runner-up activity
              4. If runner-up also booked → check the backup activity
              5. Always confirm booking details: venue, date, time, party size, confirmation number

            Never book more than one activity unless the user explicitly asked for multiple.
            Present the final booking as a clear confirmation summary.
            """)
    String reserve(String reservationRequest);
}
