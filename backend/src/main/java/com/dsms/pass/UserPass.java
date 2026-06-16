package com.dsms.pass;

import com.dsms.user.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "user_passes")
public class UserPass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pass_type_id", nullable = false)
    private PassType passType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserPassStatus status;

    @Column(name = "remaining_visits")
    private Integer remainingVisits;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_until", nullable = false)
    private Instant validUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserPass() {
    }

    public UserPass(User user, PassType passType, Instant validFrom) {
        this.user = user;
        this.passType = passType;
        this.status = UserPassStatus.ACTIVE;
        this.remainingVisits = passType.getType() == PassTypeKind.LIMITED
                ? passType.getVisitCount()
                : null;
        this.validFrom = validFrom;
        this.validUntil = validFrom.plusSeconds(passType.getValidityDays() * 86400L);
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

    public boolean canUseAt(Instant classTime) {
        if (status != UserPassStatus.ACTIVE
                || classTime.isBefore(validFrom)
                || classTime.isAfter(validUntil)) {
            return false;
        }
        return passType.getType() == PassTypeKind.UNLIMITED || remainingVisits > 0;
    }

    public void reserveVisit() {
        if (passType.getType() == PassTypeKind.UNLIMITED) {
            return;
        }
        if (remainingVisits == null || remainingVisits <= 0) {
            throw new IllegalStateException("Pass has no remaining visits");
        }
        remainingVisits--;
        if (remainingVisits == 0) {
            status = UserPassStatus.EXHAUSTED;
        }
    }

    public void releaseVisit() {
        if (passType.getType() == PassTypeKind.UNLIMITED) {
            return;
        }
        remainingVisits++;
        status = UserPassStatus.ACTIVE;
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

    public UserPassStatus getStatus() {
        return status;
    }

    public Integer getRemainingVisits() {
        return remainingVisits;
    }

    public Instant getValidFrom() {
        return validFrom;
    }

    public Instant getValidUntil() {
        return validUntil;
    }
}

