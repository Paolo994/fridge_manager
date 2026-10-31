package com.fridgemanager.food;

import com.fridgemanager.user.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "food_item", indexes = @Index(columnList = "owner_id,expiryDate"))
public class FoodItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FoodCategory category = FoodCategory.OTHER;

    private Double quantity;

    private String unit;

    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FoodSource source = FoodSource.MANUAL;

    private String notes;

    @Column(nullable = false)
    private Instant addedAt = Instant.now();

    protected FoodItem() {}

    public FoodItem(User owner) {
        this.owner = owner;
    }

    public Long getId() { return id; }
    public User getOwner() { return owner; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public FoodCategory getCategory() { return category; }
    public void setCategory(FoodCategory category) { this.category = category; }
    public Double getQuantity() { return quantity; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    public FoodSource getSource() { return source; }
    public void setSource(FoodSource source) { this.source = source; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getAddedAt() { return addedAt; }
}
