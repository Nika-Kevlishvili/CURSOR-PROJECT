package bg.energo.phoenix.service.contract.newVersionEvent.serviceContract;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceContractCreateNewVersionEventPublisher {
    private final ApplicationEventPublisher applicationEventPublisher;

    public void publishServiceContractCreateNewVersionEvent(ServiceContractCreateNewVersionEvent event) {
        log.info("Publishing service Contract new version creation event");
        applicationEventPublisher.publishEvent(event);
    }
}
