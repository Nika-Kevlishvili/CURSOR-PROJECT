package bg.energo.phoenix.event;

import bg.energo.phoenix.process.model.entity.Process;
import bg.energo.phoenix.process.model.request.ProcessCreatedEvent;
import bg.energo.phoenix.process.service.ProcessMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EventFactory {

    private final ProcessMapper processMapper;

    public ProcessCreatedEvent createProcessCreatedEvent(EventType eventType, Process process) {
        Event.Metadata metadata = createMetadata(eventType.name(), process.getSystemUserId());
        ProcessCreatedEvent.Payload payload = processMapper.toEventPayload(process);
        return new ProcessCreatedEvent(metadata, payload);
    }

    private Event.Metadata createMetadata(String eventType, String userId) {
        String eventId = UUID.randomUUID().toString();
        LocalDateTime currentTime = LocalDateTime.now();
        return new Event.Metadata(eventId, userId, currentTime, eventType);
    }

}
