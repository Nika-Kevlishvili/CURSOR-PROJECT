package bg.energo.phoenix.service.notifications.events;

import bg.energo.phoenix.service.notifications.enums.NotificationState;
import bg.energo.phoenix.service.notifications.enums.NotificationType;
import bg.energo.phoenix.service.notifications.interfaces.Notification;

public record NotificationEvent(
        Long entityId,
        NotificationType notificationType,
        Notification repository,
        NotificationState state
) {
}
