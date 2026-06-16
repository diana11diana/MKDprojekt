package com.dsms.pass;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "pass_types")
public class PassType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PassTypeKind type;

    @Column(name = "visit_count")
    private Integer visitCount;

    @Column(name = "validity_days", nullable = false)
    private int validityDays;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PassType() {
    }

    public PassType(
            String name,
            String description,
            PassTypeKind type,
            Integer visitCount,
            int validityDays,
            BigDecimal price
    ) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.visitCount = visitCount;
        this.validityDays = validityDays;
        this.price = price;
        this.currency = "PLN";
        this.active = true;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void update(
            String name,
            String description,
            PassTypeKind type,
            Integer visitCount,
            int validityDays,
            BigDecimal price,
            boolean active
    ) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.visitCount = visitCount;
        this.validityDays = validityDays;
        this.price = price;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public PassTypeKind getType() {
        return type;
    }

    public Integer getVisitCount() {
        return visitCount;
    }

    public int getValidityDays() {
        return validityDays;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCurrency() {
        return currency;
    }

    public boolean isActive() {
        return active;
    }
}

