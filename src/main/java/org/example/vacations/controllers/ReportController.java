package org.example.vacations.controllers;

import lombok.RequiredArgsConstructor;
import org.example.vacations.models.ReportStat;
import org.example.vacations.services.ReportService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/reports")
    public String reportsPage(Model model) {
        addCommon(model);
        return "reports";
    }

    @GetMapping("/reports/fragment")
    public String reportsFragment(Model model) {
        addCommon(model);
        return "reports :: reportModal";
    }

    private void addCommon(Model model) {
        List<ReportStat> stats = reportService.buildEmployeeStats();
        long totalVacations = stats.stream().mapToLong(ReportStat::getVacationsCount).sum();
        long totalCoverages = stats.stream().mapToLong(ReportStat::getCoveragesCount).sum();

        model.addAttribute("stats", stats);
        model.addAttribute("totalEmployees", stats.size());
        model.addAttribute("totalVacations", totalVacations);
        model.addAttribute("totalCoverages", totalCoverages);
    }
}
