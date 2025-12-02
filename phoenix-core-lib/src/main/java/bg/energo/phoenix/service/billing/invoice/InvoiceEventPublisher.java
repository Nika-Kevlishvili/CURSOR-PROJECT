package bg.energo.phoenix.service.billing.invoice;

import bg.energo.phoenix.service.billing.invoice.models.InvoiceCreationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Deprecated

public class InvoiceEventPublisher {

    private final ApplicationEventPublisher eventPublisher;


    /**
     * This method should be used for invoice number generation;
     * @deprecated
     * @see bg.energo.phoenix.service.billing.invoice.numberGeneration.InvoiceNumberService
     * @param invoices
     */
    public void publishInvoiceEvent(Set<Long> invoices) {
        eventPublisher.publishEvent(new InvoiceCreationEvent(invoices));
    }
}
