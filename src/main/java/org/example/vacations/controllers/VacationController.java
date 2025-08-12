package org.example.vacations.controllers;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.vacations.services.EmployeeService;
import org.example.vacations.services.VacationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class VacationController {

    private final VacationService vacationService;
    private final EmployeeService employeeService;

    @PostMapping("/request")
    public String createRequest(@RequestParam Long employeeId,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                HttpServletRequest req,
                                RedirectAttributes ra) {
        try {
            String baseUrl = req.getRequestURL().toString().replace(req.getRequestURI(), "");
            var saved = vacationService.createRequest(employeeId, startDate, endDate, baseUrl);
            ra.addFlashAttribute("success", "Vacation request created.");
            return "redirect:/request/" + saved.getId();
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/";
        }
    }

    @GetMapping("/request/{id}")
    public String requestDetail(@PathVariable Long id, Model model) {
        var r = vacationService.getRequest(id);
        var accounts = vacationService.requesterAccounts(r);

        model.addAttribute("employees", employeeService.allExcept(r.getRequester().getId()));

        Map<Long, String> coveredBy = r.getCoverages().stream()
                .collect(Collectors.toMap(c -> c.getAccount().getId(),
                        c -> c.getCoveringEmployee().getName()));

        model.addAttribute("req", r);
        model.addAttribute("accounts", accounts);
        model.addAttribute("coveredBy", coveredBy);
        return "request-detail";
    }

    @PostMapping("/request/{id}/cover")
    public String cover(@PathVariable Long id,
                        @RequestParam Long accountId,
                        @RequestParam Long coveringEmployeeId,
                        RedirectAttributes ra) {
        try {
            vacationService.coverAccount(id, accountId, coveringEmployeeId);
            ra.addFlashAttribute("success", "Coverage updated. Thanks for supporting the team!");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/request/" + id;
    }
}
