package org.example.vacations.controllers;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.vacations.models.Account;
import org.example.vacations.models.VacationRequest;
import org.example.vacations.services.EmployeeService;
import org.example.vacations.services.VacationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

//Purpose: Manages user interaction, request vacation, cover someone else, view stats, etc
@Controller
@RequiredArgsConstructor
public class VacationController {

    private final VacationService vacationService;
    private final EmployeeService employeeService;

    @PostMapping("/request") // Handles the form submission to create a new vacation request
    public String createRequest(@RequestParam Long employeeId,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                HttpServletRequest req,
                                RedirectAttributes ra,
                                org.springframework.security.core.Authentication auth) {
        try {
            String baseUrl = req.getRequestURL().toString().replace(req.getRequestURI(), "");
            var saved = vacationService.createRequest(employeeId, startDate, endDate, baseUrl); // validate, save, email

            ra.addFlashAttribute("success", "Vacation request created.");

            // If an ADMIN is logged in, send them to the request detail; public users go back to index
            boolean isAdmin = auth != null && auth.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

            return isAdmin ? "redirect:/request/" + saved.getId()
                    : "redirect:/";
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/";
        }
    }

    @GetMapping("/request/{id}")  //Loads the detail page of a specific vacation request.
    public String requestDetail(@PathVariable Long id, Model model) {
        var r = vacationService.getRequest(id);
        var accounts = vacationService.requesterAccounts(r); // Returns the list of accounts the employee to be covered during vacation.
        model.addAttribute("employees", employeeService.allExcept(r.getRequester().getId()));

        Map<Long, String> coveredBy = r.getCoverages().stream() // Pull the list of people to cover but the requester.
                .collect(Collectors.toMap(c -> c.getAccount().getId(),
                        c -> c.getCoveringEmployee().getName()));

        model.addAttribute("req", r);
        model.addAttribute("accounts", accounts);
        model.addAttribute("coveredBy", coveredBy);
        return "request-detail"; // Shows results in frontend
    }

    @PostMapping("/request/{id}/cover") // manages coverage
    public String cover(@PathVariable Long id,
                        @RequestParam Long accountId,
                        @RequestParam Long coveringEmployeeId,
                        HttpServletRequest req,
                        RedirectAttributes ra) {
        try {
            // baseUrl for deep link in emails
            String baseUrl = req.getRequestURL().toString().replace(req.getRequestURI(), "");
            vacationService.coverAccount(id, accountId, coveringEmployeeId, baseUrl); // Validates & saves + send emails
            ra.addFlashAttribute("success", "TAM assigned correctly!");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/request/" + id;
    }
    // Endpoint that allows non admin users to view current status of the request

    @GetMapping("/request/{id}/view")
    public String viewRequest(@PathVariable Long id, Model model) {
        VacationRequest req = vacationService.getRequest(id);
        List<Account> accounts = vacationService.requesterAccounts(req);

        // Build coverage map using the same logic as requestDetail method
        Map<Long, String> coveredBy = req.getCoverages().stream()
                .collect(Collectors.toMap(
                        c -> c.getAccount().getId(),
                        c -> c.getCoveringEmployee().getName()
                ));

        model.addAttribute("req", req);
        model.addAttribute("accounts", accounts);
        model.addAttribute("coveredBy", coveredBy);
        model.addAttribute("viewOnly", true);

        return "request-detail-noadmin";
    }
}
