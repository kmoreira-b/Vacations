package org.example.vacations.repos;

import org.example.vacations.models.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CoverageRepository extends JpaRepository<Coverage, Long> {
    Optional<Coverage> findByRequestIdAndAccountId(Long requestId, Long accountId);
    boolean existsByRequestIdAndAccountId(Long requestId, Long accountId);
    long countByRequestId(Long requestId);
    List<Coverage> findByRequestId(Long requestId);
    // ---------- COUNTS PER COVERING EMPLOYEE ----------
    // JPQL (use this if your entity has field 'coveringEmployee' referencing Employee)
    @Query("select c.coveringEmployee.id, count(c) from Coverage c group by c.coveringEmployee.id")
    List<Object[]> countCoveragesByEmployee();

}
