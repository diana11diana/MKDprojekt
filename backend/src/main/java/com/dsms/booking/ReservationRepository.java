package com.dsms.booking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Optional<Reservation> findByUserIdAndClassSessionId(Long userId, Long classSessionId);

    List<Reservation> findByUserIdOrderByClassSessionStartAtDesc(Long userId);
}

