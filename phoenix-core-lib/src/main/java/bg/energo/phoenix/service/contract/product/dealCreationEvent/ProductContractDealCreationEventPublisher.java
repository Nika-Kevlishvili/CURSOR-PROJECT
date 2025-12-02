package bg.energo.phoenix.service.contract.product.dealCreationEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductContractDealCreationEventPublisher {
    private final ApplicationEventPublisher applicationEventPublisher;

    public void publishProductContractDealCreationEvent(ProductContractDealCreationEvent event) {
        log.info("Publishing Product Contract Deal creation event");
        applicationEventPublisher.publishEvent(event);
    }
}
