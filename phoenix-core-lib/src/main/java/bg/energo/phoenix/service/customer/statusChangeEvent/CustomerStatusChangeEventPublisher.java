package bg.energo.phoenix.service.customer.statusChangeEvent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;
@Component
@Slf4j
public class CustomerStatusChangeEventPublisher {
    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    public void publishCustomerStatusChangeEvent(final List<Long> customerDetailIds) {
        log.info("Publishing custom event. ");
        CustomerEventModel customerEventModel = new CustomerEventModel(customerDetailIds);
        applicationEventPublisher.publishEvent(customerEventModel);
    }
}
