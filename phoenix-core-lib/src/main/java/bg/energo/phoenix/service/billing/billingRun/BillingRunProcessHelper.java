package bg.energo.phoenix.service.billing.billingRun;

import bg.energo.phoenix.billingRun.model.entity.BillingRunContracts;
import bg.energo.phoenix.billingRun.repository.BillingRunContractsRepository;
import bg.energo.phoenix.model.entity.billing.billingRun.BillingRun;
import bg.energo.phoenix.model.entity.billing.invoice.Invoice;
import bg.energo.phoenix.model.enums.billing.billings.BillingStatus;
import bg.energo.phoenix.repository.billing.billingRun.BillingRunRepository;
import bg.energo.phoenix.repository.billing.invoice.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingRunProcessHelper {
    private final BillingRunRepository billingRunRepository;
    private final BillingRunContractsRepository billingRunContractsRepository;
    private final InvoiceRepository invoiceRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateBillingRunImmediately(BillingRun billingRun) {
        log.debug("Updating billing run immediately");
        billingRunRepository.saveAndFlush(billingRun);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateBillingRunContractsImmediately(BillingRunContracts billingRun) {
        log.debug("Updating billing run contracts immediately");
        billingRunContractsRepository.saveAndFlush(billingRun);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BillingStatus getBillingStatus(BillingRun billingRun) {
        log.debug("Getting billing status for billing run {}", billingRun.getId());
        return billingRunRepository.getBillingRunCurrentStatusById(billingRun.getId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateInvoiceImmediately(Invoice invoice) {
        log.debug("Updating invoice immediately");
        invoiceRepository.saveAndFlush(invoice);
    }
}
