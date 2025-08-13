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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        event.setSummary("Vacación — " + employeeName);

        StringBuilder desc = new StringBuilder("Estado: Pendiente de cobertura\n");
        if (notes != null && !notes.trim().isEmpty()) desc.append("Notas: ").append(notes).append("\n");
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

        // Update description
        String baseDesc = event.getDescription() == null ? "" :
                event.getDescription().replaceAll("(?m)^Estado:.*$", "Estado: Con cobertura");
        String coverageLine = "Asignado: " + assigneeName +
                (accountLabel != null && !accountLabel.trim().isEmpty() ? " · Cuenta: " + accountLabel : "");
        String newDesc = (baseDesc.isEmpty() ? "" : baseDesc + "\n") + coverageLine;
        event.setDescription(newDesc);

        // Update structured properties
        Event.ExtendedProperties props = event.getExtendedProperties();
        if (props == null) props = new Event.ExtendedProperties();
        Map<String, String> priv = props.getPrivate();
        if (priv == null) priv = new HashMap<>();
        priv.put("assignee", assigneeName);
        if (accountLabel != null) priv.put("account", accountLabel);
        props.setPrivate(priv);
        event.setExtendedProperties(props);

        // Optional: add assignee as attendee if not present
        if (assigneeEmail != null && !assigneeEmail.trim().isEmpty()) {
            List<EventAttendee> attendees = event.getAttendees();
            if (attendees == null) attendees = new ArrayList<>();
            boolean exists = false;
            for (EventAttendee a : attendees) {
                if (assigneeEmail.equalsIgnoreCase(a.getEmail())) { exists = true; break; }
            }
            if (!exists) {
                attendees.add(new EventAttendee().setEmail(assigneeEmail).setDisplayName(assigneeName));
            }
            event.setAttendees(attendees);
        }

        calendar.events().update(calendarId, eventId, event).execute();
    }
}
