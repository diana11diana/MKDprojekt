package com.dsms.review;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByUserIdAndClassSessionId(Long userId, Long classSessionId);

    List<Review> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Review> findByClassSessionInstructorIdOrderByCreatedAtDesc(Long instructorId);

    List<Review> findAllByOrderByCreatedAtDesc();
}
