package com.angular.backend.boat;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BoatReservationRepository extends JpaRepository<BoatReservation, Long> {

    @Query("""
            select case when count(r) > 0 then true else false end
            from BoatReservation r
            where r.boat.id = :boatId
              and r.startDateTime < :endDateTime
              and r.endDateTime > :startDateTime
            """)
    boolean existsOverlapping(Long boatId, LocalDateTime startDateTime, LocalDateTime endDateTime);

    List<BoatReservation> findByStartDateTimeGreaterThanEqualAndStartDateTimeLessThanOrderByStartDateTimeAsc(
            LocalDateTime startDateTime,
            LocalDateTime endDateTime);
}
