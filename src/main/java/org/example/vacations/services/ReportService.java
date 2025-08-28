package org.example.vacations.services;

import lombok.RequiredArgsConstructor;
import org.example.vacations.models.Coverage;
import org.example.vacations.models.Employee;
import org.example.vacations.models.ReportStat;
import org.example.vacations.models.VacationRequest;
import org.example.vacations.repos.CoverageRepository;
import org.example.vacations.repos.EmployeeRepository;
import org.example.vacations.repos.VacationRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final EmployeeRepository employeeRepository;
    private final VacationRequestRepository vacationRequestRepository;
    private final CoverageRepository coverageRepository;

    // true = Mon–Fri only; false = calendar days
    private static final boolean BUSINESS_DAYS_ONLY = true; // If true, counts only weekdays (Mon–Fri) when calculating how many days someone took vacation or covered.

    @Transactional(readOnly = true)
    public List<ReportStat> buildEmployeeStats() {
        // Vacation count
        Map<Long, Long> vacations = vacationRequestRepository.countRequestsByRequester() // Count vacation requests per employee
                //  This tells us How many vacation requests has each person made
                .stream().collect(Collectors.toMap(
                        r -> ((Number) r[0]).longValue(),
                        r -> ((Number) r[1]).longValue()
                ));
        // Coverage count
        Map<Long, Long> coverages = coverageRepository.countCoveragesByEmployee() // Count coverages per employee
                // How many times has each person covered for someone else
                .stream().collect(Collectors.toMap(
                        r -> ((Number) r[0]).longValue(),
                        r -> ((Number) r[1]).longValue()
                ));

        // Calculate total days covered and received (account × day)
        Map<Long, Long> coveredSlotDays = new HashMap<>();   // Days you cover
        Map<Long, Long> receivedSlotDays = new HashMap<>();  // Days others cover for you

        for (Coverage cov : coverageRepository.findAll()) { // Loop calculates TAM Days Covered = business days × number of accounts covered
            VacationRequest vr = cov.getRequest();
            if (vr == null || vr.getStartDate() == null || vr.getEndDate() == null) continue;

            // Account-days (TAM days covered and received)
            long days = countDays(vr.getStartDate(), vr.getEndDate()); // Calculates how many weekdays (Mon–Fri) are in the vacation request.
            Long covererId   = cov.getCoveringEmployee().getId(); // the person covering
            Long requesterId = vr.getRequester().getId();        // the one on vacation // Who is helping vs. who is being helped.

            coveredSlotDays.merge(covererId, days, Long::sum);   //  Days TAM Covered total
            receivedSlotDays.merge(requesterId, days, Long::sum); // Days Received Coverage total // Accumulate total days covered / received per person.
        }

        // (C) assemble rows
        List<Employee> employees = employeeRepository.findAll();
        List<ReportStat> stats = new ArrayList<>(employees.size());
        for (Employee e : employees) {
            stats.add(ReportStat.builder()
                    .employeeId(e.getId())
                    .employeeName(e.getName())
                    .vacationsCount(vacations.getOrDefault(e.getId(), 0L))
                    .coveragesCount(coverages.getOrDefault(e.getId(), 0L))
                    .daysCoveredForOthers(coveredSlotDays.getOrDefault(e.getId(), 0L))
                    .daysOthersCoveredMe(receivedSlotDays.getOrDefault(e.getId(), 0L))
                    .build());
        }

        // sort by top offender
        stats.sort(
                Comparator.comparingLong(ReportStat::getCoveragesCount)
                        .thenComparing(Comparator.comparingLong(ReportStat::getVacationsCount).reversed())
                        .thenComparing(ReportStat::getEmployeeName, String.CASE_INSENSITIVE_ORDER)
        );
        return stats;
    }


    private long countDays(LocalDate start, LocalDate end) {
        if (start == null || end == null || end.isBefore(start)) return 0;
        if (!BUSINESS_DAYS_ONLY) return end.toEpochDay() - start.toEpochDay() + 1;
        long days = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            DayOfWeek w = d.getDayOfWeek();
            if (w != DayOfWeek.SATURDAY && w != DayOfWeek.SUNDAY) days++;
        }
        return days;
    }
}
