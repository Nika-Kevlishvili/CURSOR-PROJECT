package bg.energo.phoenix.service.notifications.service;

import bg.energo.phoenix.service.notifications.enums.NotificationType;

public record NotificationModel(
        Long accountManagerId,
        Long entityId,
        NotificationType notificationType
) {
}
