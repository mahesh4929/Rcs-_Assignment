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


@RestController
@RequestMapping("/api/claims")
public class ClaimController {

    private final AllocationService allocationService;
    private final ClaimRepository claimRepository;

    public ClaimController(AllocationService allocationService, ClaimRepository claimRepository) {
        this.allocationService = allocationService;
        this.claimRepository = claimRepository;
    }


    @PostMapping("/allocate")
    public Map<String, Object> allocate(
            @Valid @RequestBody ClaimRequest request) {
        Claim claim = allocationService.allocate(request);

        Map<String, Object> response = toMap(claim);

        response.put(
                "message", "Claim allocated successfully");
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
