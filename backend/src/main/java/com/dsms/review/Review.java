package com.dsms.review;

import com.dsms.schedule.ClassSession;
import com.dsms.user.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "class_reviews")
public class Review {

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
    private int rating;

    @Column(length = 2000)
    private String comment;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<ReviewReply> replies = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Review() {
    }

    public Review(User user, ClassSession classSession, int rating, String comment) {
        this.user = user;
        this.classSession = classSession;
        this.rating = rating;
        this.comment = comment;
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

    public ReviewReply addReply(User author, String body) {
        ReviewReply reply = new ReviewReply(this, author, author.getRole(), body);
        replies.add(reply);
        return reply;
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

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public List<ReviewReply> getReplies() {
        return replies;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
