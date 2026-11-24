package com.bajaj.rcs.controller;

import com.bajaj.rcs.dto.RcsDto.ClaimRequest;
import com.bajaj.rcs.entity.Claim;
import com.bajaj.rcs.repository.ClaimRepository;
import com.bajaj.rcs.service.AllocationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.bajaj.rcs.dto.RcsDto.AllocationResult;


@RestController
@RequestMapping("/api/claims")
public class ClaimController {

    private final AllocationService allocationService;
    private final ClaimRepository claimRepository;

    public ClaimController(AllocationService allocationService, ClaimRepository claimRepository) {
        this.allocationService = allocationService;
        this.claimRepository = claimRepository;
    }


    /**
     * Client / external claim system sends claim details here.
     * The claim is saved, all eligible handlers are listed, and one is picked by round robin.
     */
    @PostMapping("/allocate")
    public Map<String, Object> allocate(@Valid @RequestBody ClaimRequest request) {
        AllocationResult result = allocationService.allocate(request);

        Map<String, Object> response = toMap(result.claim());

        // List of every eligible handler for this claim
        List<Map<String, Object>> eligible = result.eligibleHandlers().stream()
                .map(h -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("handlerCode", h.getHandlerCode());
                    m.put("handlerName", h.getHandlerName());
                    return m;
                })
                .toList();
        response.put("eligibleHandlerCount", eligible.size());
        response.put("eligibleHandlers", eligible);

        if (result.alreadyAllocated()) {
            response.put("message", "Claim was already allocated - same handler returned");
        } else if (eligible.size() == 1) {
            response.put("message", "Only one eligible handler - claim allocated to it");
        } else {
            response.put("message", eligible.size() + " eligible handlers - selected by round robin");
        }
        return response;
    }

    @GetMapping("/{claimNumber}")
    public Map<String, Object> getClaim(
            @PathVariable String claimNumber) {

        Claim claim = claimRepository.findByClaimNumber(claimNumber)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Claim not found: " + claimNumber));
        return toMap(claim);
    }

    //Get All claims
    @GetMapping
    public List<Map<String, Object>> getAllClaims() {
        return claimRepository.findAll().stream().map(this::toMap).toList();
    }

    // All claims allocated to one handler
    @GetMapping("/handler/{handlerId}")
    public List<Map<String, Object>> getClaimsByHandler(@PathVariable Long handlerId) {
        return claimRepository.findByHandlerId(handlerId).stream().map(this::toMap).toList();
    }

    private Map<String, Object> toMap(Claim claim) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("claimNumber", claim.getClaimNumber());
        map.put("claimLocation", claim.getClaimLocation());
        map.put("lob", claim.getLob());
        map.put("policyNumber", claim.getPolicyNumber());
        map.put("amount", claim.getLossReserveAmount());
        map.put("status", claim.getStatus());
        if (claim.getHandler() != null) {
            map.put("handlerCode", claim.getHandler().getHandlerCode());
            map.put("handlerName", claim.getHandler().getHandlerName());
        } else {
            map.put("handlerCode", null);
            map.put("handlerName", "Not allocated");
        }
        return map;
    }
}
