package org.example.vacations.controllers;

import lombok.RequiredArgsConstructor;
import org.example.vacations.services.SecurityConfig;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequiredArgsConstructor
public class LoginController {

    @GetMapping("/login")
    public String loginPage(HttpServletRequest request, HttpSession session,
                            @RequestParam(value = "redirect", required = false) String redirect) {
        // Optional: store a safe, relative redirect for post-login
        if (redirect != null && redirect.startsWith("/")) {
            session.setAttribute("redirectAfterLogin", redirect);
        }
        return "login"; // templates/login.html
    }
}
