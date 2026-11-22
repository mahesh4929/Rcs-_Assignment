package com.bajaj.rcs.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "allocation")
public class Allocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "claim_id")
    private Claim claim;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "handler_id")
    private ClaimHandler handler;


    @Column(nullable = false)
    private long openClaimsAtAllocation;

    @Column(nullable = false)
    private LocalDateTime allocatedAt;

    protected Allocation() { }

    public Allocation(Claim claim, ClaimHandler handler, long openClaimsAtAllocation, LocalDateTime allocatedAt) {
        this.claim = claim;
        this.handler = handler;
        this.openClaimsAtAllocation = openClaimsAtAllocation;
        this.allocatedAt = allocatedAt;
    }

    public Long getId()                    { return id; }
    public long getOpenClaimsAtAllocation() { return openClaimsAtAllocation; }
    public LocalDateTime getAllocatedAt()  { return allocatedAt; }
}
