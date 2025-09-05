package org.example.vacations.services;

import lombok.RequiredArgsConstructor;
import org.example.vacations.models.Employee;
import org.example.vacations.repos.EmployeeRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VacationsUserDetailsService implements UserDetailsService {

    private final EmployeeRepository employeeRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // We log in by email
        String email = username == null ? "" : username.trim();
        Employee emp = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No employee with email " + email));

        String hash = emp.getPassword();
        if (hash == null || hash.isBlank()) {
            throw new UsernameNotFoundException("Employee has no password set: " + email);
        }

        String role = (emp.getRole() == null || emp.getRole().isBlank())
                ? "USER" : emp.getRole().trim().toUpperCase();

        boolean enabled = (emp.getEnabled() != null) ? emp.getEnabled() : true;

        GrantedAuthority auth = new SimpleGrantedAuthority("ROLE_" + role);
        return new User(emp.getEmail(), hash, enabled, true, true, true, List.of(auth));
    }
}
