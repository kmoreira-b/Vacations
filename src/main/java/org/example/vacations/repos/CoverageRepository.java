package org.example.vacations.repos;

import org.example.vacations.models.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CoverageRepository extends JpaRepository<Coverage, Long> {
    Optional<Coverage> findByRequestIdAndAccountId(Long requestId, Long accountId);
    boolean existsByRequestIdAndAccountId(Long requestId, Long accountId);
    long countByRequestId(Long requestId);
    List<Coverage> findByRequestId(Long requestId);
}
