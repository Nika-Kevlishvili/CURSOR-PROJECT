package bg.energo.phoenix.service.billing.billingRun.actions.startBilling;

import bg.energo.phoenix.billingRun.service.BillingRunStandardPreparationService;
import bg.energo.phoenix.model.entity.billing.billingRun.BillingRun;
import bg.energo.phoenix.model.enums.billing.billings.BillingStatus;
import bg.energo.phoenix.model.enums.billing.billings.BillingType;
import bg.energo.phoenix.repository.billing.billingRun.BillingRunRepository;
import bg.energo.phoenix.service.billing.billingRun.actions.BillingRunActionInvoker;
import bg.energo.phoenix.service.billing.billingRunProcess.BillingRunManualDebitOrCreditNoteProcess;
import bg.energo.phoenix.service.billing.billingRunProcess.BillingRunManualInterimAdvancePaymentProcess;
import bg.energo.phoenix.service.billing.billingRunProcess.BillingRunManualInvoiceProcess;
import bg.energo.phoenix.service.billing.invoice.reversal.InvoiceReversalProcessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingRunStartBillingInvokeService implements BillingRunActionInvoker {
    private final BillingRunRepository billingRunRepository;
    private final BillingRunStandardPreparationService preparationService;
    private final InvoiceReversalProcessService invoiceReversalProcessService;
    private final BillingRunManualInvoiceProcess billingRunManualInvoiceProcess;
    private final BillingRunManualDebitOrCreditNoteProcess billingRunManualDebitOrCreditNoteProcess;
    private final BillingRunManualInterimAdvancePaymentProcess billingRunManualInterimAdvancePaymentProcess;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void invoke(BillingRun billingRun) {
        Long billingRunId = billingRun.getId();

        log.debug("Billing type is: {}", billingRun.getType());
        if (List.of(BillingType.STANDARD_BILLING, BillingType.INVOICE_CORRECTION).contains(billingRun.getType())) {
            switch (billingRun.getType()) {
                case STANDARD_BILLING -> {
                    log.debug("Starting data preparation for billing run with id: {}", billingRunId);
                    preparationService.startDataPreparation(billingRun);
                }
                case INVOICE_CORRECTION -> {
                    log.debug("Starting invoice correction process for billing run with id: {}", billingRunId);
                    preparationService.startDataPreparationCorrection(billingRun);
                }
            }
        } else {
            if (Objects.equals(billingRun.getType(), BillingType.INVOICE_REVERSAL)) {
                log.debug("Starting invoice reversal process for billing run with id: {}", billingRunId);

                log.debug("Saving data for invoice reversal process for billing run with id: %s (type: %s)".formatted(billingRunId, billingRun.getType()));
                invoiceReversalProcessService.saveData(billingRun);

                log.debug("Starting invoice reversal process for billing run with id: %s (type: %s)".formatted(billingRunId, billingRun.getType()));
                invoiceReversalProcessService.startProcessing(billingRun);
            } else {
                switch (billingRun.getType()) {
                    case MANUAL_INVOICE -> {
                        log.debug("Starting manual invoice process for billing run with id: {}", billingRunId);
                        billingRunManualInvoiceProcess.process(billingRun);
                    }
                    case MANUAL_CREDIT_OR_DEBIT_NOTE -> {
                        log.debug("Starting manual credit or debit note process for billing run with id: {}", billingRunId);
                        billingRunManualDebitOrCreditNoteProcess.process(billingRun);
                    }
                    case MANUAL_INTERIM_AND_ADVANCE_PAYMENT -> {
                        log.debug("Starting manual interim and advance payment process for billing run with id: {}", billingRunId);
                        billingRunManualInterimAdvancePaymentProcess.process(billingRun);
                    }
                }
            }

            log.debug("Setting billing run status to 'DRAFT'");
            billingRun.setStatus(BillingStatus.DRAFT);
            billingRunRepository.save(billingRun);
        }
    }
}
