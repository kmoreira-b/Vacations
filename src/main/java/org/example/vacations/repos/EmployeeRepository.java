package org.example.vacations.repos;

import org.example.vacations.models.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByName(String name);
    Optional<Employee> findByEmail(String email);
}
