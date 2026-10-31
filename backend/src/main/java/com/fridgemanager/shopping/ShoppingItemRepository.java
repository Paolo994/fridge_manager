package com.fridgemanager.shopping;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShoppingItemRepository extends JpaRepository<ShoppingItem, Long> {
    List<ShoppingItem> findByOwnerIdOrderByCheckedAscCreatedAtDesc(Long ownerId);
    Optional<ShoppingItem> findByIdAndOwnerId(Long id, Long ownerId);
    boolean existsByOwnerIdAndNameIgnoreCaseAndCheckedFalse(Long ownerId, String name);
    void deleteByOwnerIdAndCheckedTrue(Long ownerId);
}
