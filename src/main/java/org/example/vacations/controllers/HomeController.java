package org.example.vacations.controllers;

import lombok.RequiredArgsConstructor;
import org.example.vacations.services.EmployeeService;
import org.example.vacations.services.VacationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final EmployeeService employeeService;
    private final VacationService vacationService;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("employees", employeeService.all());
        model.addAttribute("requests", vacationService.allRequests());
        return "index";
    }
}
