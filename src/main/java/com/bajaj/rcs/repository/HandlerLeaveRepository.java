package com.bajaj.rcs.repository;

import com.bajaj.rcs.entity.HandlerLeave;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface HandlerLeaveRepository extends JpaRepository<HandlerLeave, Long> {


    boolean existsByHandlerIdAndLeaveFromLessThanEqualAndLeaveToGreaterThan(
            Long handlerId, LocalDateTime now, LocalDateTime sameNow);

    List<HandlerLeave> findByHandlerId(Long handlerId);
}
