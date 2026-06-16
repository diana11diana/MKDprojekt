package com.dsms.pass;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;

public interface UserPassRepository extends JpaRepository<UserPass, Long> {

    List<UserPass> findByUserIdOrderByValidUntilDesc(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select pass from UserPass pass
            join fetch pass.passType type
            where pass.user.id = :userId
              and pass.status = com.dsms.pass.UserPassStatus.ACTIVE
              and pass.validFrom <= :classTime
              and pass.validUntil >= :classTime
            order by pass.validUntil asc
            """)
    List<UserPass> findUsableCandidatesForUpdate(
            @Param("userId") Long userId,
            @Param("classTime") Instant classTime
    );
}
