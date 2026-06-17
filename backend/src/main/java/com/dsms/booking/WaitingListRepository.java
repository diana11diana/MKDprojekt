package com.dsms.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("""
            select entry from WaitingListEntry entry
            join fetch entry.user user
            where entry.classSession.id = :classSessionId
              and entry.status = :status
            order by entry.position asc
            """)
    List<WaitingListEntry> findInstructorEntries(
            @Param("classSessionId") Long classSessionId,
            @Param("status") WaitingListStatus status
    );
}
