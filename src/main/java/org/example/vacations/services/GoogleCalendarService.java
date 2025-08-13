package org.example.vacations.services;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

@Service
public class GoogleCalendarService {

    private final Calendar calendar;
    private final String calendarId;
    private final ZoneId zoneId;

    public GoogleCalendarService(Calendar calendar,
                                 @Value("${calendar.id}") String calendarId,
                                 @Value("${app.timezone}") String tz) {
        this.calendar = calendar;
        this.calendarId = calendarId;
        this.zoneId = ZoneId.of(tz);
    }

    public String createVacationEvent(String employeeName,
                                      String employeeEmail,
                                      ZonedDateTime start,
                                      ZonedDateTime end,
                                      String vacationId,
                                      String notes) throws Exception {

        // ---- Build event ----
        Event event = new Event();
        event.setSummary("OOTO Approved — " + employeeName);

        StringBuilder desc = new StringBuilder("Status: Pending coverage\n");
        if (notes != null && !notes.trim().isEmpty()) desc.append("Notes: ").append(notes).append("\n");
        if (vacationId != null && !vacationId.trim().isEmpty()) desc.append("VacationsApp ID: ").append(vacationId);
        event.setDescription(desc.toString());

        // Extended properties (structured data)
        Event.ExtendedProperties ext = new Event.ExtendedProperties();
        Map<String, String> priv = new HashMap<>();
        priv.put("vacationId", vacationId == null ? "" : vacationId);
        priv.put("assignee", "");

        priv.put("account", "");
        ext.setPrivate(priv);
        event.setExtendedProperties(ext);

        // Optional attendee (use plain list for compatibility)
        if (employeeEmail != null && !employeeEmail.trim().isEmpty()) {
            List<EventAttendee> attendees = new ArrayList<>();
            attendees.add(new EventAttendee()
                    .setEmail(employeeEmail)
                    .setDisplayName(employeeName));
            event.setAttendees(attendees);
        }

        // Times
        EventDateTime startDT = new EventDateTime()
                .setDateTime(new DateTime(start.withZoneSameInstant(zoneId).toInstant().toEpochMilli()))
                .setTimeZone(zoneId.getId());
        EventDateTime endDT = new EventDateTime()
                .setDateTime(new DateTime(end.withZoneSameInstant(zoneId).toInstant().toEpochMilli()))
                .setTimeZone(zoneId.getId());
        event.setStart(startDT);
        event.setEnd(endDT);

        Event created = calendar.events().insert(calendarId, event).execute();
        return created.getId(); // store this
    }

    public void updateCoverage(String eventId,
                               String assigneeName,
                               String accountLabel,
                               String assigneeEmail) throws Exception {

        Event event = calendar.events().get(calendarId, eventId).execute();

        // Normalize/flip status line to "Status: Covered" (handles English or Spanish)
        String desc = event.getDescription() == null ? "" : event.getDescription();
        desc = desc.replaceAll("(?m)^(Estado|Status):.*$", "Status: Covered");

        // Build the coverage line: "<covering person>: <Account only>"
        String shortAccount = (accountLabel == null) ? "" : accountLabel.trim();
        shortAccount = shortAccount.replaceFirst("^.*?\\s*-\\s*", "");
        String coverageLine = assigneeName + ": " + shortAccount;

        // Avoid duplicate lines
        boolean exists = false;
        for (String line : desc.split("\\r?\\n")) {
            if (line.trim().equalsIgnoreCase(coverageLine)) { exists = true; break; }
        }
        if (!exists) {
            desc = (desc.isEmpty() ? coverageLine : desc + "\n" + coverageLine);
        }
        event.setDescription(desc);

        // Update structured properties (keep last assignee/account for convenience)
        Event.ExtendedProperties props = event.getExtendedProperties();
        if (props == null) props = new Event.ExtendedProperties();
        Map<String, String> priv = props.getPrivate();
        if (priv == null) priv = new HashMap<>();
        priv.put("assignee", assigneeName);
        if (accountLabel != null) priv.put("account", shortAccount);
        props.setPrivate(priv);
        event.setExtendedProperties(props);

        // Optional: add assignee as attendee if not present
        if (assigneeEmail != null && !assigneeEmail.trim().isEmpty()) {
            List<EventAttendee> attendees = event.getAttendees();
            if (attendees == null) attendees = new ArrayList<>();
            boolean found = false;
            for (EventAttendee a : attendees) {
                if (assigneeEmail.equalsIgnoreCase(a.getEmail())) { found = true; break; }
            }
            if (!found) {
                attendees.add(new EventAttendee().setEmail(assigneeEmail).setDisplayName(assigneeName));
            }
            event.setAttendees(attendees);
        }

        calendar.events().update(calendarId, eventId, event).execute();
    }


    /** Look up the event by our private extended property "vacationId", then update it. */
    public void updateCoverageByVacationId(String vacationId,
                                           String assigneeName,
                                           String accountLabel,
                                           String assigneeEmail) throws Exception {
        // Find the event we created for this vacation request
        List<Event> items = calendar.events().list(calendarId)
                .setPrivateExtendedProperty(Collections.singletonList("vacationId=" + vacationId))
                .setSingleEvents(true)
                .setMaxResults(50)
                .execute()
                .getItems();

        if (items == null || items.isEmpty()) {
            return; // nothing to update
        }

        String eventId = items.get(0).getId();
        updateCoverage(eventId, assigneeName, accountLabel, assigneeEmail);
    }
}
