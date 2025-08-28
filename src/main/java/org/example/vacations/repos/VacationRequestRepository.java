package org.example.vacations.repos;

import org.example.vacations.models.VacationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface VacationRequestRepository extends JpaRepository<VacationRequest, Long> {
    List<VacationRequest> findByRequesterIdOrderByStartDateDesc(Long requesterId);
    List<VacationRequest> findAllByOrderByStartDateDesc();

    // Overlap check: existing.end >= new.start AND existing.start <= new.end
    boolean existsByRequesterIdAndEndDateGreaterThanEqualAndStartDateLessThanEqual(
            Long requesterId, LocalDate start, LocalDate end);

    // ---------- COUNTS PER REQUESTER ---------- Do not remove
    @Query("select vr.requester.id, count(vr) from VacationRequest vr group by vr.requester.id")
    List<Object[]> countRequestsByRequester();
}

