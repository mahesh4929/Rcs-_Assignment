package com.bajaj.rcs.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;




@Entity
@Table(name = "claim_handler")
public class ClaimHandler {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String handlerCode;

    @Column(nullable = false, length = 100)
    private String handlerName;

    @Column(nullable = false, length = 20)
    private String lob;


    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal minAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal maxAmount;

    @Column(nullable = false)
    private boolean active = true;


    @JsonIgnore
    @OneToMany(mappedBy = "handler", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HandlerMapping> mappings = new ArrayList<>();

    protected ClaimHandler() { }

    public ClaimHandler(String handlerCode, String handlerName, String lob,
                        BigDecimal minAmount, BigDecimal maxAmount, boolean active) {
        this.handlerCode = handlerCode;
        this.handlerName = handlerName;
        this.lob = lob;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.active = active;
    }


    public void update(String handlerName, String lob, BigDecimal minAmount, BigDecimal maxAmount, boolean active) {
        this.handlerName = handlerName;
        this.lob = lob;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.active = active;
    }


    public void replaceMappings(String type, List<String> values) {
        List<String> wanted = values == null ? List.of()
                : values.stream().filter(v -> v != null && !v.isBlank())
                        .map(v -> v.trim().toUpperCase()).toList();
        mappings.removeIf(m -> m.getMappingType().equals(type) && !wanted.contains(m.getMappingValue()));
        wanted.forEach(v -> addMapping(type, v));
    }


    public void addMapping(String type, String value) {
        if (value == null || value.isBlank()) return;
        String clean = value.trim().toUpperCase();
        if (!valuesOf(type).contains(clean)) {
            mappings.add(new HandlerMapping(this, type, clean));
        }
    }


    public boolean matches(Claim claim) {
        if (!active) return false;
        if (!lob.equalsIgnoreCase(claim.getLob())) return false;

        BigDecimal amount = claim.getLossReserveAmount();
        if (amount.compareTo(minAmount) < 0 || amount.compareTo(maxAmount) > 0) return false;

        // Claim location must be one of the handler's locations.
        if (!contains(getLocations(), claim.getClaimLocation())) return false;

        // Policy numbers / insured names: none mapped = any; otherwise must be in the list.
        if (!getPolicyNumbers().isEmpty() && !contains(getPolicyNumbers(), claim.getPolicyNumber())) return false;
        return getInsuredNames().isEmpty() || contains(getInsuredNames(), claim.getInsuredName());
    }

    private static boolean contains(List<String> values, String claimValue) {
        return claimValue != null && values.contains(claimValue.trim().toUpperCase());
    }

    private List<String> valuesOf(String type) {
        return mappings.stream()
                .filter(m -> m.getMappingType().equals(type))
                .map(HandlerMapping::getMappingValue)
                .toList();
    }

    // These three show up as lists in the JSON response.
    public List<String> getLocations()     { return valuesOf("LOCATION"); }
    public List<String> getPolicyNumbers() { return valuesOf("POLICY_NUMBER"); }
    public List<String> getInsuredNames()  { return valuesOf("INSURED_NAME"); }

    public Long getId()              { return id; }
    public String getHandlerCode()   { return handlerCode; }
    public String getHandlerName()   { return handlerName; }
    public String getLob()           { return lob; }
    public BigDecimal getMinAmount() { return minAmount; }
    public BigDecimal getMaxAmount() { return maxAmount; }
    public boolean isActive()        { return active; }
}
