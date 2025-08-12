package org.example.vacations.services;

import lombok.RequiredArgsConstructor;
import org.example.vacations.models.Employee;
import org.example.vacations.repos.EmployeeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository repo;

    public List<Employee> all() {
        return repo.findAll();
    }

    public Employee get(Long id) {
        return repo.findById(id).orElseThrow();
    }

    // <<< NOT static as would error out n the controller >>>
    public List<Employee> allExcept(Long id) {
        return repo.findAll().stream()
                .filter(e -> !e.getId().equals(id))
                .toList();
    }
}
