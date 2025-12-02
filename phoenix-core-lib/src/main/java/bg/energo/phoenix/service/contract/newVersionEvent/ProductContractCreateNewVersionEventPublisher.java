package bg.energo.phoenix.service.contract.newVersionEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductContractCreateNewVersionEventPublisher {
    private final ApplicationEventPublisher applicationEventPublisher;

    public void publishProductContractCreateNewVersionEvent(ProductContractCreateNewVersionEvent event) {
        log.info("Publishing Product Contract new version creation event");
        applicationEventPublisher.publishEvent(event);
    }
}
