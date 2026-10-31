package com.fridgemanager.shopping;

import com.fridgemanager.user.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "shopping_item")
public class ShoppingItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(nullable = false)
    private String name;

    private String quantity;

    @Column(nullable = false)
    private boolean checked;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected ShoppingItem() {}

    public ShoppingItem(User owner, String name, String quantity) {
        this.owner = owner;
        this.name = name;
        this.quantity = quantity;
    }

    public Long getId() { return id; }
    public User getOwner() { return owner; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getQuantity() { return quantity; }
    public void setQuantity(String quantity) { this.quantity = quantity; }
    public boolean isChecked() { return checked; }
    public void setChecked(boolean checked) { this.checked = checked; }
    public Instant getCreatedAt() { return createdAt; }
}
