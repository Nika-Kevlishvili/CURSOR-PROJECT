package bg.energo.scheduler.schedulers;

import bg.energo.phoenix.model.customAnotations.aspects.ExecutionTimeLogger;
import bg.energo.phoenix.service.notifications.service.NotificationCleanerService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Profile({"dev", "test","preProd", "prod"})
@ConditionalOnExpression("${app.cfg.schedulers.enabled:true}")
public class NotificationCleanerScheduler {
    private final NotificationCleanerService cleanerService;

    @Scheduled(cron = "@daily")
    @ExecutionTimeLogger(value = "NotificationCleanerScheduler")
    public void cleanupOutdatedUserNotifications() {
        cleanerService.clean();
    }
}
