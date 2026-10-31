package com.fridgemanager.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findTop50ByOwnerIdOrderByCreatedAtDesc(Long ownerId);
    List<Notification> findByOwnerIdAndReadFalse(Long ownerId);
    long countByOwnerIdAndReadFalse(Long ownerId);
    Optional<Notification> findByIdAndOwnerId(Long id, Long ownerId);
    boolean existsByOwnerIdAndDedupKey(Long ownerId, String dedupKey);
}
