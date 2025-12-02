package bg.energo.phoenix.service.billing.billingRun.errors;

import bg.energo.phoenix.model.entity.billing.billingRun.BillingErrorData;
import bg.energo.phoenix.model.enums.billing.billings.BillingStatus;
import bg.energo.phoenix.repository.billing.billingRun.BillingErrorDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static bg.energo.phoenix.model.enums.billing.billings.BillingStatus.COMPLETED;
import static bg.energo.phoenix.model.enums.billing.billings.BillingStatus.IN_PROGRESS_ACCOUNTING;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingRunErrorService {
    private final ApplicationEventPublisher eventPublisher;
    private final BillingErrorDataRepository errorDataRepository;

    public void publishBillingErrors(List<InvoiceErrorShortObject> exception, Long billingId, BillingStatus status) {
        eventPublisher.publishEvent(new BillingErrorEvent(billingId, exception, convertBillingStatusToProtocol(status)));
        log.debug("Error event was published!;");
    }

    private BillingProtocol convertBillingStatusToProtocol(BillingStatus status) {
        if (status.equals(IN_PROGRESS_ACCOUNTING) || status.equals(COMPLETED)) {
            return BillingProtocol.ACCOUNTING;
        } else {
            return BillingProtocol.BILLING;
        }
    }

    @EventListener
    @Async
    public void billingErrorListener(BillingErrorEvent event) {
        List<BillingErrorData> billingErrorData = new ArrayList<>();
        Long billingId = event.getBillingId();
        List<InvoiceErrorShortObject> errorMessages = event.getErrorMessages();
        errorMessages.forEach(x -> {
            billingErrorData.add(new BillingErrorData(null, x.getInvoiceNumber(), x.getErrorMessage(), event.getProtocol(), billingId));
        });
        errorDataRepository.saveAll(billingErrorData);
    }
}
