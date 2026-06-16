package com.dsms.pass;

import com.dsms.user.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "pass_orders")
public class PassOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pass_type_id", nullable = false)
    private PassType passType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_pass_id")
    private UserPass userPass;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PassOrderStatus status;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PassOrder() {
    }

    public PassOrder(User user, PassType passType) {
        this.user = user;
        this.passType = passType;
        this.status = PassOrderStatus.PENDING_PAYMENT;
        this.amount = passType.getPrice();
        this.currency = passType.getCurrency();
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

    public boolean isPending() {
        return status == PassOrderStatus.PENDING_PAYMENT;
    }

    public void markPaid(UserPass userPass, String paymentReference) {
        this.userPass = userPass;
        this.paymentReference = paymentReference;
        this.status = PassOrderStatus.PAID;
        this.paidAt = Instant.now();
    }

    public void cancel() {
        status = PassOrderStatus.CANCELLED;
        cancelledAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public PassType getPassType() {
        return passType;
    }

    public UserPass getUserPass() {
        return userPass;
    }

    public PassOrderStatus getStatus() {
        return status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
