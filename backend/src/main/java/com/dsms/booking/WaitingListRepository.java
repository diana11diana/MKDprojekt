package com.dsms.booking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WaitingListRepository extends JpaRepository<WaitingListEntry, Long> {

    Optional<WaitingListEntry> findByUserIdAndClassSessionId(Long userId, Long classSessionId);

    Optional<WaitingListEntry> findFirstByClassSessionIdAndStatusOrderByPositionAsc(
            Long classSessionId,
            WaitingListStatus status
    );

    Optional<WaitingListEntry> findFirstByClassSessionIdOrderByPositionDesc(Long classSessionId);

    List<WaitingListEntry> findByUserIdAndStatusOrderByClassSessionStartAtAsc(
            Long userId,
            WaitingListStatus status
    );
}

