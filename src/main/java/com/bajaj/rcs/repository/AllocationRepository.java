package com.bajaj.rcs.repository;

import com.bajaj.rcs.entity.Allocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AllocationRepository extends JpaRepository<Allocation, Long> {

    /** The most recent allocation a handler received (used for round robin). */
    Optional<Allocation> findTopByHandlerIdOrderByIdDesc(Long handlerId);
}