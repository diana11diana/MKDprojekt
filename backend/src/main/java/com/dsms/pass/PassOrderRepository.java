package com.dsms.pass;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PassOrderRepository extends JpaRepository<PassOrder, Long> {

    List<PassOrder> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<PassOrder> findAllByOrderByCreatedAtDesc();

    Optional<PassOrder> findByIdAndUserId(Long id, Long userId);
}
