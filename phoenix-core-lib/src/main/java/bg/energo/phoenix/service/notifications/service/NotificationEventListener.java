package bg.energo.phoenix.service.notifications.service;

import bg.energo.phoenix.exception.IllegalArgumentsProvidedException;
import bg.energo.phoenix.service.notifications.enums.NotificationType;
import bg.energo.phoenix.service.notifications.events.NotificationEvent;
import bg.energo.phoenix.service.notifications.interfaces.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEventListener {
    private final NotificationService notificationService;

    @Transactional
    @EventListener(NotificationEvent.class)
    public void listenNotificationEvent(NotificationEvent notificationEvent) {
        try {
            if (Stream.of(
                    notificationEvent.entityId(),
                    notificationEvent.notificationType(),
                    notificationEvent.repository()).anyMatch(Objects::isNull)) {
                log.error("Invalid event for notification, all parameters should be filled");
                throw new IllegalArgumentsProvidedException("Invalid event for notification, all parameters should be filled");
            }

            Long entityId = notificationEvent.entityId();
            NotificationType notificationType = notificationEvent.notificationType();
            Notification repository = notificationEvent.repository();

            List<Long> notificationTargets =
                    repository
                            .getNotificationTargets(
                                    entityId,
                                    notificationEvent.state() == null ? null : notificationEvent.state().name()
                            )
                            .stream()
                            .filter(Objects::nonNull)
                            .toList();

            if (CollectionUtils.isNotEmpty(notificationTargets)) {
                notificationService.sendNotifications(notificationTargets.stream().map(nt -> new NotificationModel(nt, entityId, notificationType)).toList());
            }
        } catch (Exception e) {
            log.error("Notifications cannot be saved", e);
        }
    }
}
