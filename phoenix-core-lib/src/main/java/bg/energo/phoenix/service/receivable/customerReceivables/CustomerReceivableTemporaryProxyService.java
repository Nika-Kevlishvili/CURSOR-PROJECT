package bg.energo.phoenix.service.receivable.customerReceivables;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.entity.billing.invoice.Invoice;
import bg.energo.phoenix.model.entity.receivable.CustomerReceivable;
import bg.energo.phoenix.model.enums.receivable.LiabilityOrReceivableCreationSource;
import bg.energo.phoenix.repository.billing.invoice.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Deprecated(forRemoval = true)
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerReceivableTemporaryProxyService {
    private final InvoiceRepository invoiceRepository;
    private final CustomerReceivableService customerReceivableService;

    @Transactional
    public Long createCustomerReceivable(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Invoice with id " + invoiceId + " not found"));
        CustomerReceivable customerReceivable = customerReceivableService.createFromInvoice(invoice, LiabilityOrReceivableCreationSource.BILLING_RUN);
        return customerReceivable.getId();
    }
}
