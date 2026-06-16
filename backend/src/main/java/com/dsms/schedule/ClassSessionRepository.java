package com.dsms.schedule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;

public interface ClassSessionRepository extends JpaRepository<ClassSession, Long> {

    List<ClassSession> findByStatusAndStartAtAfterOrderByStartAtAsc(
            ClassStatus status,
            Instant startAt
    );

    List<ClassSession> findAllByOrderByStartAtDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from ClassSession session where session.id = :id")
    java.util.Optional<ClassSession> findByIdForUpdate(@Param("id") Long id);
}
