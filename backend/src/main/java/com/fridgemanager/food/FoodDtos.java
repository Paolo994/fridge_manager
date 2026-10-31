package com.fridgemanager.food;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class FoodDtos {

    private FoodDtos() {}

    public record FoodRequest(@NotBlank @Size(max = 120) String name,
                              FoodCategory category,
                              @PositiveOrZero Double quantity,
                              @Size(max = 20) String unit,
                              LocalDate expiryDate,
                              FoodSource source,
                              @Size(max = 500) String notes) {}

    public record FoodResponse(Long id, String name, FoodCategory category, Double quantity, String unit,
                               LocalDate expiryDate, Long daysToExpiry, ExpiryStatus status,
                               FoodSource source, String notes, Instant addedAt) {

        public static FoodResponse of(FoodItem f) {
            Long days = f.getExpiryDate() == null ? null
                    : ChronoUnit.DAYS.between(LocalDate.now(), f.getExpiryDate());
            return new FoodResponse(f.getId(), f.getName(), f.getCategory(), f.getQuantity(), f.getUnit(),
                    f.getExpiryDate(), days, ExpiryStatus.of(days), f.getSource(), f.getNotes(), f.getAddedAt());
        }
    }

    public enum ExpiryStatus {
        EXPIRED, EXPIRING_SOON, OK, UNKNOWN;

        static ExpiryStatus of(Long days) {
            if (days == null) return UNKNOWN;
            if (days < 0) return EXPIRED;
            if (days <= 2) return EXPIRING_SOON;
            return OK;
        }
    }
}
