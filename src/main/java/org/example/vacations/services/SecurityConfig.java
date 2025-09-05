package org.example.vacations.services;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final VacationsUserDetailsService userDetailsService;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authProvider() {
        DaoAuthenticationProvider p = new DaoAuthenticationProvider();
        p.setUserDetailsService(userDetailsService);
        p.setPasswordEncoder(passwordEncoder());
        return p;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // SavedRequest handler (fallback target)
        var successHandler = new SavedRequestAwareAuthenticationSuccessHandler();
        successHandler.setDefaultTargetUrl("/");

        // Do not cache /error or static resources as the "saved request"
        var requestCache = new HttpSessionRequestCache() {
            @Override
            public void saveRequest(HttpServletRequest request, HttpServletResponse response) {
                String uri = request.getRequestURI();
                if (uri.startsWith("/error") || uri.startsWith("/css/") || uri.startsWith("/js/")) {
                    return; // skip caching these
                }
                super.saveRequest(request, response);
            }
        };

        http
                .authenticationProvider(authProvider())
                .requestCache(rc -> rc.requestCache(requestCache))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/css/**", "/js/**", "/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/request").permitAll()
                        // This allows ONLY the view endpoint for everyone
                        .requestMatchers(HttpMethod.GET, "/request/*/view").permitAll()
                        // This still blocks all OTHER /request/** paths for non-admins
                        .requestMatchers("/request/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(login -> login
                        .loginPage("/login").permitAll()
                        .loginProcessingUrl("/login")
                        .failureUrl("/login?error")
                        .successHandler((req, res, auth) -> {
                            HttpSession session = req.getSession(false);
                            String target = (session != null) ? (String) session.getAttribute("redirectAfterLogin") : null;
                            if (target != null && target.startsWith("/")) {
                                session.removeAttribute("redirectAfterLogin");
                                res.sendRedirect(target);
                                return;
                            }
                            var sr = requestCache.getRequest(req, res);
                            if (sr != null && sr.getRedirectUrl() != null && sr.getRedirectUrl().contains("/error")) {
                                res.sendRedirect("/");
                                return;
                            }
                            successHandler.onAuthenticationSuccess(req, res, auth);
                        })
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }
}
