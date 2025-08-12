package org.example.vacations.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vacations.models.Account;
import org.example.vacations.models.Employee;
import org.example.vacations.repos.AccountRepository;
import org.example.vacations.repos.EmployeeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final EmployeeRepository employeeRepository;
    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (employeeRepository.count() > 0) return;

        // 5 people + 5 accounts each (hardcoded)
        List<Employee> people = employeeRepository.saveAll(List.of(
                Employee.builder().name("Alex Vega").email("kmoreirab@ucenfotec.ac.cr").build(),
                Employee.builder().name("Bianca Ruiz").email("kmoreirab@ucenfotec.ac.cr").build(),
                Employee.builder().name("Carlos Mora").email("kmoreirab@ucenfotec.ac.cr").build(),
                Employee.builder().name("Diana Solis").email("kmoreirab@ucenfotec.ac.cr").build(),
                Employee.builder().name("Ethan Cruz").email("kmoreirab@ucenfotec.ac.cr").build()
        ));

        people.forEach(e -> {
            for (int i = 1; i <= 5; i++) {
                accountRepository.save(Account.builder()
                        .name(e.getName() + " - Account " + i)
                        .owner(e)
                        .build());
            }
        });

        log.info("Seeded {} employees and {} accounts", employeeRepository.count(), accountRepository.count());
    }
}
