package com.bajaj.rcs.service;

import com.bajaj.rcs.dto.RcsDto.AllocationResult;
import com.bajaj.rcs.dto.RcsDto.ClaimRequest;
import com.bajaj.rcs.entity.Allocation;
import com.bajaj.rcs.entity.Claim;
import com.bajaj.rcs.entity.ClaimHandler;
import com.bajaj.rcs.repository.AllocationRepository;
import com.bajaj.rcs.repository.ClaimHandlerRepository;
import com.bajaj.rcs.repository.ClaimRepository;
import com.bajaj.rcs.repository.HandlerLeaveRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Service layer - business logic and eligibility rules.
 *
 * 1. Save the claim sent by the client.
 * 2. Find ALL eligible handlers.
 * 3. One eligible -> that one. More than one -> ROUND ROBIN.
 */
@Service
public class AllocationService {

    private final ClaimRepository claimRepository;
    private final ClaimHandlerRepository handlerRepository;
    private final HandlerLeaveRepository leaveRepository;
    private final AllocationRepository allocationRepository;

    public AllocationService(ClaimRepository claimRepository,
                             ClaimHandlerRepository handlerRepository,
                             HandlerLeaveRepository leaveRepository,
                             AllocationRepository allocationRepository) {
        this.claimRepository = claimRepository;
        this.handlerRepository = handlerRepository;
        this.leaveRepository = leaveRepository;
        this.allocationRepository = allocationRepository;
    }

    @Transactional
    public AllocationResult allocate(ClaimRequest request) {

        // Save the claim (or reuse it if the client sends the same claim again).
        Claim claim = claimRepository.findByClaimNumber(request.claimNumber())
                .orElseGet(() -> claimRepository.save(new Claim(
                        request.claimNumber(), request.claimLocation(), request.lob(),
                        request.policyNumber(), request.insuredName(), request.lossReserveAmount())));

        // Step 1: list of ALL eligible handlers.
        LocalDateTime now = LocalDateTime.now();
        List<ClaimHandler> eligible = handlerRepository.findByActiveTrue().stream()
                .filter(h -> isEligible(h, claim, now))
                .sorted(Comparator.comparing(ClaimHandler::getId))
                .toList();

        // Already allocated - keep the same handler instead of allocating twice.
        if (claim.getHandler() != null) {
            return new AllocationResult(claim, eligible, true);
        }

        if (eligible.isEmpty()) {
            throw new IllegalStateException("No eligible handler for claim " + claim.getClaimNumber());
        }

        // Step 2: one eligible -> that one. More than one -> round robin.
        ClaimHandler chosen = eligible.size() == 1 ? eligible.get(0) : nextByRoundRobin(eligible);

        claim.allocateTo(chosen);
        allocationRepository.save(new Allocation(claim, chosen, openClaimCount(chosen), now));
        return new AllocationResult(claim, eligible, false);
    }

    /**
     * Round robin: whoever got a claim longest ago gets the next one.
     * Never allocated = goes first.
     * Example with A, B, C eligible:  A -> B -> C -> A -> B -> C ...
     */
    private ClaimHandler nextByRoundRobin(List<ClaimHandler> eligible) {
        ClaimHandler chosen = null;
        Long chosenLastAllocation = null;

        for (ClaimHandler handler : eligible) {
            Long lastAllocation = allocationRepository.findTopByHandlerIdOrderByIdDesc(handler.getId())
                    .map(Allocation::getId)
                    .orElse(null);                       // null = never allocated

            if (lastAllocation == null) {
                return handler;                          // never allocated -> its turn now
            }
            if (chosen == null || lastAllocation < chosenLastAllocation) {
                chosen = handler;
                chosenLastAllocation = lastAllocation;
            }
        }
        return chosen;
    }

    private boolean isEligible(ClaimHandler handler, Claim claim, LocalDateTime now) {
        if (!handler.matches(claim)) return false;   // active, LOB, amount, location, policy, insured
        return !leaveRepository.existsByHandlerIdAndLeaveFromLessThanEqualAndLeaveToGreaterThan(
                handler.getId(), now, now);
    }

    private long openClaimCount(ClaimHandler handler) {
        return claimRepository.countByHandlerIdAndStatus(handler.getId(), "OPEN");
    }
}