package bg.energo.phoenix.service.notifications.interfaces;

import java.util.List;

public interface Notification {
    List<Long> getNotificationTargets(Long entityId, String notificationState);
}
