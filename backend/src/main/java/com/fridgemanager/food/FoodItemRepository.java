package com.fridgemanager.food;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FoodItemRepository extends JpaRepository<FoodItem, Long> {

    @Query("select f from FoodItem f where f.owner.id = :ownerId " +
           "order by case when f.expiryDate is null then 1 else 0 end, f.expiryDate, f.name")
    List<FoodItem> findAllForOwner(Long ownerId);

    Optional<FoodItem> findByIdAndOwnerId(Long id, Long ownerId);

    @Query("select f from FoodItem f join fetch f.owner where f.expiryDate is not null and f.expiryDate <= :limit")
    List<FoodItem> findExpiringBefore(LocalDate limit);
}
