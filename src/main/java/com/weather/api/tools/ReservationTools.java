package com.weather.api.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Random;

/**
 * Mock reservation system. In a real system these would call a booking API
 * (e.g. Fareharbor, Checkfront, Rezdy, or a custom venue API).
 */
@Component
public class ReservationTools {

    private final Random random = new Random();

    @Tool("Check whether a venue has availability for a given date and party size. " +
          "Returns available time slots or the next open date if fully booked.")
    public String checkAvailability(
            @P("Venue or activity name") String venue,
            @P("Requested date in YYYY-MM-DD format") String date,
            @P("Number of people in the party") int partySize) {

        // Simulate 80% chance available (deterministic for same venue+date in real systems)
        boolean available = (venue.hashCode() + date.hashCode()) % 5 != 0;
        int spotsLeft = available ? random.nextInt(8) + partySize : 0;

        if (available) {
            return String.format("""
                    ✓ %s is AVAILABLE on %s
                      Spots remaining : %d total (%d for your party)
                      Available slots : 9:00 AM · 11:00 AM · 2:00 PM · 4:30 PM
                      Price estimate  : $%d–%d per person
                    """,
                    venue, date, spotsLeft, partySize,
                    30 + random.nextInt(20), 50 + random.nextInt(20));
        } else {
            String nextDate = LocalDate.parse(date).plusDays(1).toString();
            return String.format("""
                    ✗ %s is FULLY BOOKED on %s
                      Next open date  : %s
                      Waitlist        : Available (typical wait ~45 min)
                      Tip             : Try the 9:00 AM slot on %s — least busy
                    """,
                    venue, date, nextDate, nextDate);
        }
    }

    @Tool("Make a confirmed reservation at a venue for a specific date, time, and party size.")
    public String makeReservation(
            @P("Venue or activity name") String venue,
            @P("Date in YYYY-MM-DD format") String date,
            @P("Preferred time slot, e.g. '2:00 PM'") String timeSlot,
            @P("Number of people") int partySize,
            @P("Name the reservation should be made under") String reservationName) {

        String confirmationNumber = "WA-" + String.format("%06d", Math.abs(
                (venue + date + timeSlot).hashCode() % 1_000_000));
        double costPerPerson = 30 + random.nextInt(30);
        double totalCost = costPerPerson * partySize;

        return String.format("""
                ✅ RESERVATION CONFIRMED
                ─────────────────────────────────
                Confirmation #  : %s
                Activity        : %s
                Date            : %s at %s
                Party Size      : %d %s
                Reserved Under  : %s
                Total Cost      : $%.2f  ($%.0f/person)
                ─────────────────────────────────
                Meeting Point   : Main entrance — look for the %s flag
                What to Bring   : Comfortable shoes, water, sunscreen
                Cancellation    : Free cancellation until 24 h before
                Support         : bookings@weatheragent.example.com
                """,
                confirmationNumber, venue, date, timeSlot,
                partySize, partySize == 1 ? "person" : "people",
                reservationName, totalCost, costPerPerson,
                venue.split(" ")[0]);
    }
}
