package com.fridgemanager.notification;

import com.fridgemanager.user.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "notification",
       uniqueConstraints = @UniqueConstraint(columnNames = {"owner_id", "dedupKey"}))
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(nullable = false, length = 500)
    private String message;

    private Long foodItemId;

    /** Evita di creare due volte lo stesso avviso (es. "item 12 scade, livello WARNING"). */
    @Column(nullable = false)
    private String dedupKey;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected Notification() {}

    public Notification(User owner, String message, Long foodItemId, String dedupKey) {
        this.owner = owner;
        this.message = message;
        this.foodItemId = foodItemId;
        this.dedupKey = dedupKey;
    }

    public Long getId() { return id; }
    public User getOwner() { return owner; }
    public String getMessage() { return message; }
    public Long getFoodItemId() { return foodItemId; }
    public String getDedupKey() { return dedupKey; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public Instant getCreatedAt() { return createdAt; }
}
