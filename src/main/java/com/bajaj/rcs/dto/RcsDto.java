package com.bajaj.rcs.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


public final class RcsDto {

    private RcsDto() { }

    // Claim details sent by the client / external claim system.
    public record ClaimRequest(
            @NotBlank String claimNumber,
            @NotBlank String claimLocation,
            @NotBlank String lob,
            String policyNumber,
            String insuredName,
            @NotNull @DecimalMin("0.01") BigDecimal lossReserveAmount
    ) { }

    // Create a claim handler. One handler can have many locations and many policy numbers.
    public record HandlerRequest(
            @NotBlank String handlerCode,
            @NotBlank String handlerName,
            @NotEmpty List<String> locations,
            @NotBlank String lob,
            @NotNull BigDecimal minAmount,
            @NotNull BigDecimal maxAmount,
            List<String> policyNumbers,      // empty = handler takes any policy
            List<String> insuredNames,       // empty = handler takes any insured
            boolean active,
            @Valid LeaveRequest leaveRequest   // optional: leave / half day when creating or updating
    ) { }


    // "Add" button: add more locations / policy numbers / insured names to a handler.
    public record MappingRequest(
            @NotBlank @Pattern(regexp = "LOCATION|POLICY_NUMBER|INSURED_NAME",
                    message = "must be LOCATION, POLICY_NUMBER or INSURED_NAME") String mappingType,
            @NotEmpty List<String> values
    ) { }

    // Leave or half day window
    public record LeaveRequest(
            @NotNull LocalDateTime leaveFrom,
            @NotNull LocalDateTime leaveTo,
            boolean halfDay
    ) { }
}
