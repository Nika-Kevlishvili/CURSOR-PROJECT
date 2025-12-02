package bg.energo.phoenix.model.response.notification;

import bg.energo.phoenix.model.entity.notification.UserNotification;
import bg.energo.phoenix.service.notifications.enums.NotificationType;

import java.time.LocalDateTime;

public record UserNotificationResponse(
        Long id,
        Long entityId,
        NotificationType notificationType,
        LocalDateTime createDate,
        Boolean readen,
        LocalDateTime readDate
) {
    public UserNotificationResponse(UserNotification userNotification) {
        this(
                userNotification.getId(),
                userNotification.getEntityId(),
                userNotification.getNotificationType(),
                userNotification.getCreateDate(),
                userNotification.getIsReaden(),
                userNotification.getReadDate()
        );
    }

    public UserNotificationResponse(Long id,
                                    Long entityId,
                                    NotificationType notificationType,
                                    LocalDateTime createDate,
                                    Boolean readen,
                                    LocalDateTime readDate) {
        this.id = id;
        this.entityId = entityId;
        this.notificationType = notificationType;
        this.createDate = createDate;
        this.readen = readen;
        this.readDate = readDate;
    }
}
