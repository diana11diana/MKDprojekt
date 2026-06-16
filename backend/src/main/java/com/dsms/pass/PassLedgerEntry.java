package com.dsms.pass;

import com.dsms.booking.Reservation;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "pass_ledger_entries")
public class PassLedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_pass_id", nullable = false)
    private UserPass userPass;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LedgerEntryType type;

    @Column(name = "visit_delta", nullable = false)
    private int visitDelta;

    @Column(name = "balance_after")
    private Integer balanceAfter;

    @Column(length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PassLedgerEntry() {
    }

    public PassLedgerEntry(
            UserPass userPass,
            Reservation reservation,
            LedgerEntryType type,
            int visitDelta,
            String reason
    ) {
        this.userPass = userPass;
        this.reservation = reservation;
        this.type = type;
        this.visitDelta = visitDelta;
        this.balanceAfter = userPass.getRemainingVisits();
        this.reason = reason;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}

