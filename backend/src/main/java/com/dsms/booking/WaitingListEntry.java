package com.dsms.booking;

import com.dsms.schedule.ClassSession;
import com.dsms.user.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "waiting_list")
public class WaitingListEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_session_id", nullable = false)
    private ClassSession classSession;

    @Column(nullable = false)
    private int position;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WaitingListStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WaitingListEntry() {
    }

    public WaitingListEntry(User user, ClassSession classSession, int position) {
        this.user = user;
        this.classSession = classSession;
        this.position = position;
        this.status = WaitingListStatus.WAITING;
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

    public void promote() {
        status = WaitingListStatus.PROMOTED;
    }

    public void cancel() {
        status = WaitingListStatus.CANCELLED;
    }

    public void expire() {
        status = WaitingListStatus.EXPIRED;
    }

    public void rejoin(int position) {
        this.position = position;
        this.status = WaitingListStatus.WAITING;
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

    public int getPosition() {
        return position;
    }

    public WaitingListStatus getStatus() {
        return status;
    }
}
