package bg.energo.phoenix.service.notifications.service;

import bg.energo.phoenix.repository.notification.UserNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationCleanerService {
    private final NotificationProperties notificationProperties;
    private final UserNotificationRepository userNotificationRepository;

    @Async
    @Transactional
    public void clean() {
        LocalDateTime unreadenNotificationsDeadline = LocalDateTime
                .now()
                .minusMonths(notificationProperties.getUnreadenNotificationDeadlineInMonths())
                .minusDays(1);

        LocalDateTime readenNotificationsDeadline = LocalDateTime
                .now()
                .minusMonths(notificationProperties.getReadenNotificationDeadlineInMonths())
                .minusDays(1);

        userNotificationRepository.deleteOutDatedUserNotifications(unreadenNotificationsDeadline, readenNotificationsDeadline);
    }
}
