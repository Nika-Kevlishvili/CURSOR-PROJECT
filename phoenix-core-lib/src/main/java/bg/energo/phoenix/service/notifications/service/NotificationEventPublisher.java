package bg.energo.phoenix.service.notifications.service;

import bg.energo.phoenix.service.notifications.events.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEventPublisher {
    private final ApplicationEventPublisher applicationEventPublisher;

    @Async
    public void publishNotification(NotificationEvent notificationEvent) {
        log.debug("Publishing notification event: {}", notificationEvent);
        applicationEventPublisher.publishEvent(notificationEvent);
    }
}
