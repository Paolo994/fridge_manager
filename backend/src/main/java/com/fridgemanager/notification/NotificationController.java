package com.fridgemanager.notification;

import com.fridgemanager.security.AuthUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    public record NotificationDto(Long id, String message, Long foodItemId, boolean read, Instant createdAt) {
        static NotificationDto of(Notification n) {
            return new NotificationDto(n.getId(), n.getMessage(), n.getFoodItemId(), n.isRead(), n.getCreatedAt());
        }
    }

    public record NotificationsResponse(long unread, List<NotificationDto> items) {}

    private final NotificationRepository repo;

    public NotificationController(NotificationRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public NotificationsResponse list(@AuthenticationPrincipal AuthUser user) {
        return new NotificationsResponse(
                repo.countByOwnerIdAndReadFalse(user.id()),
                repo.findTop50ByOwnerIdOrderByCreatedAtDesc(user.id()).stream().map(NotificationDto::of).toList());
    }

    @PostMapping("/{id}/read")
    @Transactional
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        repo.findByIdAndOwnerId(id, user.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND))
                .setRead(true);
    }

    @PostMapping("/read-all")
    @Transactional
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead(@AuthenticationPrincipal AuthUser user) {
        repo.findByOwnerIdAndReadFalse(user.id()).forEach(n -> n.setRead(true));
    }
}
