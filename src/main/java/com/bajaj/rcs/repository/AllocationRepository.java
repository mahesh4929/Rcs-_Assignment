package com.bajaj.rcs.repository;

import com.bajaj.rcs.entity.Allocation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AllocationRepository extends JpaRepository<Allocation, Long> {
}
