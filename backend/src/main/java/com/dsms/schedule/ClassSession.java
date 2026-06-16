package com.dsms.schedule;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "class_sessions")
public class ClassSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "dance_style", nullable = false, length = 100)
    private String danceStyle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClassLevel level;

    @Column(name = "instructor_name", nullable = false, length = 200)
    private String instructorName;

    @Column(name = "instructor_id")
    private Long instructorId;

    @Column(nullable = false)
    private int capacity;

    @Column(name = "booked_places", nullable = false)
    private int bookedPlaces;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClassStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ClassSession() {
    }

    public ClassSession(
            String title,
            String description,
            String danceStyle,
            ClassLevel level,
            Long instructorId,
            String instructorName,
            int capacity,
            Instant startAt,
            int durationMinutes
    ) {
        this.title = title;
        this.description = description;
        this.danceStyle = danceStyle;
        this.level = level;
        this.instructorId = instructorId;
        this.instructorName = instructorName;
        this.capacity = capacity;
        this.bookedPlaces = 0;
        this.startAt = startAt;
        this.durationMinutes = durationMinutes;
        this.status = ClassStatus.DRAFT;
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
            String title,
            String description,
            String danceStyle,
            ClassLevel level,
            Long instructorId,
            String instructorName,
            int capacity,
            Instant startAt,
            int durationMinutes
    ) {
        this.title = title;
        this.description = description;
        this.danceStyle = danceStyle;
        this.level = level;
        this.instructorId = instructorId;
        this.instructorName = instructorName;
        this.capacity = capacity;
        this.startAt = startAt;
        this.durationMinutes = durationMinutes;
    }

    public void publish() {
        status = ClassStatus.PUBLISHED;
    }

    public void cancel() {
        status = ClassStatus.CANCELLED;
    }

    public boolean hasAvailablePlace() {
        return bookedPlaces < capacity;
    }

    public void occupyPlace() {
        if (!hasAvailablePlace()) {
            throw new IllegalStateException("Class is full");
        }
        bookedPlaces++;
    }

    public void releasePlace() {
        if (bookedPlaces > 0) {
            bookedPlaces--;
        }
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getDanceStyle() {
        return danceStyle;
    }

    public ClassLevel getLevel() {
        return level;
    }

    public String getInstructorName() {
        return instructorName;
    }

    public Long getInstructorId() {
        return instructorId;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getBookedPlaces() {
        return bookedPlaces;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public ClassStatus getStatus() {
        return status;
    }
}
