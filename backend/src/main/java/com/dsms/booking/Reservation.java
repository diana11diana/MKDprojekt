package com.dsms.booking;

import com.dsms.pass.UserPass;
import com.dsms.schedule.ClassSession;
import com.dsms.user.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_session_id", nullable = false)
    private ClassSession classSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_pass_id")
    private UserPass userPass;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReservationStatus status;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Reservation() {
    }

    public Reservation(User user, ClassSession classSession, UserPass userPass) {
        this.user = user;
        this.classSession = classSession;
        this.userPass = userPass;
        this.status = ReservationStatus.CONFIRMED;
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

    public void reconfirm(UserPass pass) {
        userPass = pass;
        status = ReservationStatus.CONFIRMED;
        cancelledAt = null;
    }

    public void cancel(boolean late) {
        status = late ? ReservationStatus.LATE_CANCELLED : ReservationStatus.CANCELLED;
        cancelledAt = Instant.now();
    }

    public boolean isActive() {
        return status == ReservationStatus.CONFIRMED;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public ClassSession getClassSession() {
        return classSession;
    }

    public UserPass getUserPass() {
        return userPass;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

