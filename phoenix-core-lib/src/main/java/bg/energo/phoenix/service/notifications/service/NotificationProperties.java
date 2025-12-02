package bg.energo.phoenix.service.notifications.service;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "user.notifications")
public class NotificationProperties {
    private Long unreadenNotificationDeadlineInMonths;
    private Long readenNotificationDeadlineInMonths;
}
