package com.dsms.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Optional<Reservation> findByUserIdAndClassSessionId(Long userId, Long classSessionId);

    List<Reservation> findByUserIdOrderByClassSessionStartAtDesc(Long userId);

    @Query("""
            select reservation from Reservation reservation
            join fetch reservation.user user
            left join fetch reservation.userPass userPass
            left join fetch userPass.passType passType
            where reservation.classSession.id = :classSessionId
              and reservation.status in :statuses
            order by user.lastName asc, user.firstName asc
            """)
    List<Reservation> findInstructorParticipants(
            @Param("classSessionId") Long classSessionId,
            @Param("statuses") Collection<ReservationStatus> statuses
    );
}
