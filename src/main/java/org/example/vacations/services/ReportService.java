package org.example.vacations.services;

import lombok.RequiredArgsConstructor;
import org.example.vacations.models.Employee;
import org.example.vacations.models.ReportStat;
import org.example.vacations.repos.CoverageRepository;
import org.example.vacations.repos.EmployeeRepository;
import org.example.vacations.repos.VacationRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final EmployeeRepository employeeRepository;
    private final VacationRequestRepository vacationRequestRepository;
    private final CoverageRepository coverageRepository;

    @Transactional(readOnly = true)
    public List<ReportStat> buildEmployeeStats() {
        // Get raw grouped counts (id, count) -> maps
        Map<Long, Long> vacations = vacationRequestRepository.countRequestsByRequester()
                .stream()
                .collect(Collectors.toMap(
                        r -> ((Number) r[0]).longValue(),
                        r -> ((Number) r[1]).longValue()
                ));

        Map<Long, Long> coverages = coverageRepository.countCoveragesByEmployee()
                .stream()
                .collect(Collectors.toMap(
                        r -> ((Number) r[0]).longValue(),
                        r -> ((Number) r[1]).longValue()
                ));

        // Assemble rows for every employee (0 if missing)
        List<Employee> employees = employeeRepository.findAll();
        List<ReportStat> stats = new ArrayList<>(employees.size());

        for (Employee e : employees) {
            long v = vacations.getOrDefault(e.getId(), 0L);
            long c = coverages.getOrDefault(e.getId(), 0L);
            stats.add(new ReportStat(e.getId(), e.getName(), v, c));
        }

        // Sort: lowest coverage/vacation ratio first (worst), then more vacations, then fewer coverages
        stats.sort(
                Comparator.comparingDouble(ReportStat::getCoveragePerVacation) // asc = worse first
                        .thenComparing(Comparator.comparingLong(ReportStat::getVacationsCount).reversed())
                        .thenComparing(Comparator.comparingLong(ReportStat::getCoveragesCount))
        );

        return stats;
    }
}
