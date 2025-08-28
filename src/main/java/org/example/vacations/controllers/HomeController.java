package org.example.vacations.controllers;

import lombok.RequiredArgsConstructor;
import org.example.vacations.models.ReportStat;
import org.example.vacations.services.EmployeeService;
import org.example.vacations.services.VacationService;
import org.example.vacations.services.ReportService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final EmployeeService employeeService;
    private final VacationService vacationService;
    private final ReportService reportService;

    @GetMapping("/")
    public String home(Model model) {
        // existing data
        model.addAttribute("employees", employeeService.all());
        model.addAttribute("requests", vacationService.allRequests());

        //data used by the reports modal fragment
        List<ReportStat> stats = reportService.buildEmployeeStats();
        long totalVacations = stats.stream().mapToLong(ReportStat::getVacationsCount).sum();
        long totalCoverages = stats.stream().mapToLong(ReportStat::getCoveragesCount).sum();

        model.addAttribute("stats", stats);
        model.addAttribute("totalEmployees", stats.size());
        model.addAttribute("totalVacations", totalVacations);
        model.addAttribute("totalCoverages", totalCoverages);

        return "index";
    }
}
