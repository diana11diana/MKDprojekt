package com.dsms.pass;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PassTypeRepository extends JpaRepository<PassType, Long> {

    List<PassType> findByActiveTrueOrderByPriceAsc();

    List<PassType> findAllByOrderByPriceAsc();
}

