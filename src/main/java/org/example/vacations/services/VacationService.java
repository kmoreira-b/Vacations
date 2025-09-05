package org.example.vacations.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vacations.models.*;
import org.example.vacations.repos.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Slf4j //  Lombok annotation for logging
@Service //  Spring service component
@RequiredArgsConstructor // Generates constructor for all final fields
public class VacationService {

    private final EmployeeRepository employeeRepository;
    private final AccountRepository accountRepository;
    private final VacationRequestRepository requestRepository;
    private final CoverageRepository coverageRepository;
    private final EmailService emailService;

    // Inject the calendar service
    private final GoogleCalendarService googleCalendarService;

    // Read timezone (fallback handled below)
    @Value("${app.timezone:}")
    private String appTimezone;

    @Transactional
    // Checks for valid date range (end >= start) / Uses a repository query to ensure the same employee doesn't have overlapping vacation requests
    public VacationRequest createRequest(Long requesterId, LocalDate start, LocalDate end, String baseUrl) {
        if (start == null || end == null || end.isBefore(start)) {
            throw new IllegalArgumentException("Invalid date range.");
        }
        if (start.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Start date cannot be in the past.");
        }
        // block overlap for the same employee
        if (requestRepository.existsByRequesterIdAndEndDateGreaterThanEqualAndStartDateLessThanEqual(
                requesterId, start, end)) {
            throw new IllegalArgumentException("You already have a request that overlaps those dates.");
        }

        Employee requester = employeeRepository.findById(requesterId).orElseThrow();
        VacationRequest saved = requestRepository.save(
                VacationRequest.builder()
                        .requester(requester)
                        .startDate(start)
                        .endDate(end)
                        .build()
        );

        // Link to the request detail page
        String link = baseUrl + "/request/" + saved.getId();

        // Send only ONE email (hardcoded recipient)
        String html = """
                <p><b>%s</b> requested vacation from <b>%s</b> to <b>%s</b>.</p>
                <p>Open the request: <a href="%s">%s</a></p>
                """.formatted(requester.getName(), start, end, link, link);

        emailService.sendHtml(
                "kmoreirab@ucenfotec.ac.cr",
                "Time-off Request created for " + requester.getName(),
                html
        );

        // Create Google Calendar event
        try {
            ZoneId zone = (appTimezone != null && !appTimezone.isBlank())
                    ? ZoneId.of(appTimezone)
                    : ZoneId.of("America/Costa_Rica");
            ZonedDateTime zStart = start.atStartOfDay(zone);
            ZonedDateTime zEnd   = end.plusDays(1).atStartOfDay(zone);

            googleCalendarService.createVacationEvent(
                    requester.getName(),
                    null,
                    zStart,
                    zEnd,
                    String.valueOf(saved.getId()),  // traceability in extendedProperties
                    null
            );
        } catch (Exception e) {
            log.warn("[Calendar] Failed to create event for request {}: {}", saved.getId(), e.getMessage());
        }

        log.info("VacationRequest {} created for {} {}->{}", saved.getId(), requester.getName(), start, end);
        return saved;
    }

    public VacationRequest getRequest(Long id) {
        return requestRepository.findById(id).orElseThrow();
    }

    public List<VacationRequest> allRequests() {
        return requestRepository.findAllByOrderByStartDateDesc();
    }

    public List<Account> requesterAccounts(VacationRequest r) {
        return accountRepository.findByOwnerId(r.getRequester().getId());
    }

    @Transactional
    //Two Overloaded Methods
    public void coverAccount(Long requestId, Long accountId, Long coveringEmployeeId) {
        VacationRequest req = requestRepository.findById(requestId).orElseThrow();
        Account account = accountRepository.findById(accountId).orElseThrow();

        if (!account.getOwner().getId().equals(req.getRequester().getId())) {
            throw new IllegalArgumentException("Account does not belong to requester");
        }
        if (coverageRepository.existsByRequestIdAndAccountId(requestId, accountId)) {
            return; // idempotent
        }
        if (coveringEmployeeId.equals(req.getRequester().getId())) {
            throw new IllegalArgumentException("Requester cannot cover their own accounts");
        }

        Employee covering = employeeRepository.findById(coveringEmployeeId).orElseThrow();
        coverageRepository.save(
                Coverage.builder()
                        .request(req)
                        .account(account)
                        .coveringEmployee(covering)
                        .build()
        );
        log.info("Coverage added: request {} account {} by {}", requestId, accountId, covering.getName());

        // 🔗 Update Google Calendar: append "<covering>: <account>" and flip status to "Covered"
        try {
            googleCalendarService.updateCoverageByVacationId(
                    String.valueOf(req.getId()),
                    covering.getName(),
                    account.getName(),
                    null // or covering.getEmail() to add them as an attendee
            );
        } catch (Exception e) {
            log.warn("[Calendar] Failed to update coverage for request {}: {}", req.getId(), e.getMessage());
        }
    }

    //Returns how many accounts have coverage assigned for a request.
    public long countCovered(Long requestId) {
        return coverageRepository.countByRequestId(requestId);
    }

    @Transactional
    public void coverAccount(Long requestId, Long accountId, Long coveringEmployeeId, String baseUrl) {
        VacationRequest req = requestRepository.findById(requestId).orElseThrow();
        Account account = accountRepository.findById(accountId).orElseThrow();

        if (!account.getOwner().getId().equals(req.getRequester().getId())) {
            throw new IllegalArgumentException("Account does not belong to requester");
        }
        if (coverageRepository.existsByRequestIdAndAccountId(requestId, accountId)) {
            return; // idempotent
        }
        if (coveringEmployeeId.equals(req.getRequester().getId())) {
            throw new IllegalArgumentException("Requester cannot cover their own accounts");
        }

        Employee covering = employeeRepository.findById(coveringEmployeeId).orElseThrow();
        coverageRepository.save(
                Coverage.builder()
                        .request(req)
                        .account(account)
                        .coveringEmployee(covering)
                        .build()
        );
        log.info("Coverage added: request {} account {} by {}", requestId, accountId, covering.getName());

        // 🔗 Google Calendar update
        try {
            googleCalendarService.updateCoverageByVacationId(
                    String.valueOf(req.getId()),
                    covering.getName(),
                    account.getName(),
                    null // or covering.getEmail()
            );
        } catch (Exception e) {
            log.warn("[Calendar] Failed to update coverage for request {}: {}", req.getId(), e.getMessage());
        }

        // Send coverage emails via EmailService
        emailService.sendCoverageEmails(req, account, covering, baseUrl);
    }


}
