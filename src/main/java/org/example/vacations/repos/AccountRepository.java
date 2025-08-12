package org.example.vacations.repos;

import org.example.vacations.models.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByOwnerId(Long ownerId);
    boolean existsByOwnerIdAndName(Long ownerId, String name);
}
