package com.dsms.instructor;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InstructorProfileRepository extends JpaRepository<InstructorProfile, Long> {

    List<InstructorProfile> findByPublicProfileTrueOrderByUserLastNameAsc();

    List<InstructorProfile> findAllByOrderByUserLastNameAsc();

    Optional<InstructorProfile> findByUserId(Long userId);
}

