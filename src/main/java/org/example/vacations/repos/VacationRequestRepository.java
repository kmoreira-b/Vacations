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
    // ---------- COUNTS PER REQUESTER ----------
    // JPQL (use this if your entity has field 'requester' referencing Employee)
    @Query("select vr.requester.id, count(vr) from VacationRequest vr group by vr.requester.id")
    List<Object[]> countRequestsByRequester();

    /* If your field names are different OR you prefer native SQL, comment out the JPQL above
       and uncomment this native query (adjust table/column names if needed):

    @Query(value = "select requester_id, count(*) from vacation_requests group by requester_id", nativeQuery = true)
    List<Object[]> countRequestsByRequester();
    */
}

