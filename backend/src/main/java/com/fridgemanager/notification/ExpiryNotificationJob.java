package com.fridgemanager.notification;

import com.fridgemanager.config.AppProperties;
import com.fridgemanager.food.FoodItem;
import com.fridgemanager.food.FoodItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Crea gli avvisi di scadenza. È logica deterministica: qui un agente IA non serve.
 * Gli avvisi compaiono nella campanella della webapp; email o push si possono aggiungere più avanti.
 */
@Component
public class ExpiryNotificationJob {

    private static final Logger log = LoggerFactory.getLogger(ExpiryNotificationJob.class);

    private final FoodItemRepository foods;
    private final NotificationRepository notifications;
    private final int warningDays;

    public ExpiryNotificationJob(FoodItemRepository foods, NotificationRepository notifications, AppProperties props) {
        this.foods = foods;
        this.notifications = notifications;
        this.warningDays = props.expiry().warningDays();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        run();
    }

    @Scheduled(cron = "${app.expiry.cron}")
    @Transactional
    public void run() {
        LocalDate today = LocalDate.now();
        int created = 0;
        for (FoodItem item : foods.findExpiringBefore(today.plusDays(warningDays))) {
            long days = ChronoUnit.DAYS.between(today, item.getExpiryDate());
            String level = days < 0 ? "EXPIRED" : "WARNING";
            // La chiave include la data di scadenza: se l'utente la modifica, l'avviso viene ricreato.
            String key = "food:" + item.getId() + ":" + level + ":" + item.getExpiryDate();
            Long ownerId = item.getOwner().getId();
            if (notifications.existsByOwnerIdAndDedupKey(ownerId, key)) continue;
            notifications.save(new Notification(item.getOwner(), message(item.getName(), days), item.getId(), key));
            created++;
        }
        if (created > 0) log.info("Creati {} avvisi di scadenza", created);
    }

    static String message(String name, long days) {
        if (days < 0) return name + " è scaduto da " + (-days) + (days == -1 ? " giorno" : " giorni");
        if (days == 0) return name + " scade oggi";
        if (days == 1) return name + " scade domani";
        return name + " scade tra " + days + " giorni";
    }
}
