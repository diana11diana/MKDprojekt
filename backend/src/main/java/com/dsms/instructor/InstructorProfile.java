package com.dsms.instructor;

import com.dsms.user.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "instructor_profiles")
public class InstructorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 255)
    private String specialization;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_public", nullable = false)
    private boolean publicProfile;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InstructorProfile() {
    }

    public InstructorProfile(User user, String specialization, String description) {
        this.user = user;
        this.specialization = specialization;
        this.description = description;
        this.publicProfile = true;
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

    public void update(String specialization, String description, boolean publicProfile) {
        this.specialization = specialization;
        this.description = description;
        this.publicProfile = publicProfile;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getSpecialization() {
        return specialization;
    }

    public String getDescription() {
        return description;
    }

    public boolean isPublicProfile() {
        return publicProfile;
    }
}

