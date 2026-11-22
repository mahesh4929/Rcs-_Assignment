package com.bajaj.rcs.entity;

import jakarta.persistence.*;






@Entity
@Table(name = "handler_mapping",
       uniqueConstraints = @UniqueConstraint(columnNames = {"handler_id", "mapping_type", "mapping_value"}))
public class HandlerMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "handler_id")
    private ClaimHandler handler;

    /** LOCATION, POLICY_NUMBER or INSURED_NAME */
    @Column(nullable = false, length = 20)
    private String mappingType;

    @Column(nullable = false, length = 150)
    private String mappingValue;

    protected HandlerMapping() { }

    public HandlerMapping(ClaimHandler handler, String mappingType, String mappingValue) {
        this.handler = handler;
        this.mappingType = mappingType;
        this.mappingValue = mappingValue;
    }

    public Long getId()             { return id; }
    public String getMappingType()  { return mappingType; }
    public String getMappingValue() { return mappingValue; }
}
