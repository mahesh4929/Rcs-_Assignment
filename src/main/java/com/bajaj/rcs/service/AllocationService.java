package com.bajaj.rcs.service;

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
import java.util.List;

// Save the claim sent by the client.
// Find eligible handlers: active, LOB, amount range, one of the handler's locations,
//       one of the handler's policy numbers (if any), insured name (if any), not on leave / half day.
// Allocate to the eligible handler with the fewest open claims.




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
    public Claim allocate(ClaimRequest request) {

        // Save the claim (or reuse it if the client sends the same claim again).
        Claim claim = claimRepository.findByClaimNumber(request.claimNumber())
                .orElseGet(() -> claimRepository.save(new Claim(
                        request.claimNumber(), request.claimLocation(), request.lob(),
                        request.policyNumber(), request.insuredName(), request.lossReserveAmount())));

        // Already allocated - return the same handler instead of allocating twice.
        if (claim.getHandler() != null) {
            return claim;
        }

        //1 which handlers are eligible?
        LocalDateTime now = LocalDateTime.now();
        List<ClaimHandler> eligible = handlerRepository.findByActiveTrue().stream()
                .filter(h -> isEligible(h, claim, now))
                .toList();

        if (eligible.isEmpty()) {
            throw new IllegalStateException("No eligible handler for claim " + claim.getClaimNumber());
        }

        // 2: of those, pick the one with the fewest open claims.
        ClaimHandler chosen = eligible.get(0);
        long lowestCount = openClaimCount(chosen);

        for (ClaimHandler handler : eligible) {
            long count = openClaimCount(handler);
            if (count < lowestCount) {
                lowestCount = count;
                chosen = handler;
            }
        }

        claim.allocateTo(chosen);
        allocationRepository.save(new Allocation(claim, chosen, lowestCount, now));
        return claim;
    }

    private boolean isEligible(ClaimHandler handler, Claim claim, LocalDateTime now) {
        if (!handler.matches(claim)) return false;   // active, LOB, amount, location, policy, insured
        return !leaveRepository.existsByHandlerIdAndLeaveFromLessThanEqualAndLeaveToGreaterThan(
                handler.getId(), now, now);
    }

    private long openClaimCount(ClaimHandler handler) {
        return claimRepository
                .countByHandlerIdAndStatus(handler.getId()
                        , "OPEN");
    }
}
