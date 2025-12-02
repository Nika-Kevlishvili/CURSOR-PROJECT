package bg.energo.phoenix.service.customer.statusChangeEvent;

import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.enums.customer.CustomerDetailStatus;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;


@Component
@Slf4j
@AllArgsConstructor
public class CustomerStatusChangeEventListener {
    private final CustomerDetailsRepository customerDetailsRepository;

    @EventListener
    @Async
    public void handleCustomerStatusChangeEvent(CustomerEventModel customerEventModel) {
        for (Long cdi : customerEventModel.getCustomerDetailIds()) {
            log.info("Customer status should be changed for Customer with id: " + cdi + "\n" + "result is: " + customerDetailsRepository.customerHasActiveContractsOrOrders(cdi));
            CustomerDetails customerDetails = customerDetailsRepository.findById(cdi).orElse(null);
            if (customerDetails == null)
                continue;

            boolean customerHasActiveContractsOrOrders = customerDetailsRepository.customerHasActiveContractsOrOrders(cdi);

            if (customerHasActiveContractsOrOrders) {
                log.info("Customer Detail with id: [" + customerDetails.getId() + "] has active contracts should be ACTIVE customer");
                customerDetails.setStatus(CustomerDetailStatus.ACTIVE);
            } else {
                if (customerDetailsRepository.customerHasAtListOneContractorOrderThatIsTerminated(cdi) && !customerDetailsRepository.customerHasAtListOneContractorOrderThatIsNotTerminated(cdi)){
                    log.info("Customer Detail with id: ["+ customerDetails.getId() + "] has only terminated/draft/ready contracts should be LOST customer");
                    customerDetails.setStatus(CustomerDetailStatus.LOST);
                }else{
                    log.info("Customer Detail with id: ["+ customerDetails.getId() + "] does not have any contract contract should be NEW customer");
                    customerDetails.setStatus(CustomerDetailStatus.NEW);
                }
            }
            customerDetailsRepository.save(customerDetails);
        }
    }
}
