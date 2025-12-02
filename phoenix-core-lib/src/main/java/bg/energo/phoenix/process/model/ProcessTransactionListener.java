package bg.energo.phoenix.process.model;

import bg.energo.phoenix.process.model.entity.Process;
import bg.energo.phoenix.process.service.ProcessNotificationCreationEvent;
import jakarta.persistence.PostPersist;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProcessTransactionListener {

    private final ApplicationEventPublisher eventPublisher;

    @PostPersist
    public void ProcessNotificationCreationEventListener(Process process){
        log.debug("PublishingProcessEvent");
        eventPublisher.publishEvent(new ProcessNotificationCreationEvent(process.getId()));
    }
}
