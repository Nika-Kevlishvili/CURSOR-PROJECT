package bg.energo.phoenix.service.billing.billingRun.actions.startAccounting;

import bg.energo.phoenix.model.entity.billing.billingRun.BillingRun;
import bg.energo.phoenix.model.entity.billing.invoice.Invoice;
import bg.energo.phoenix.model.enums.billing.billings.SendingAnInvoice;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceDocumentType;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStatus;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceType;
import bg.energo.phoenix.model.enums.receivable.LiabilityOrReceivableCreationSource;
import bg.energo.phoenix.repository.billing.billingRun.BillingRunInvoicesRepository;
import bg.energo.phoenix.repository.billing.billingRun.BillingRunRepository;
import bg.energo.phoenix.repository.billing.invoice.InvoiceRepository;
import bg.energo.phoenix.repository.receivable.customerLiability.CustomerLiabilityRepository;
import bg.energo.phoenix.service.billing.billingRun.BillingRunEmailSenderService;
import bg.energo.phoenix.service.billing.billingRun.BillingRunProcessHelper;
import bg.energo.phoenix.service.billing.billingRun.actions.BillingRunActionInvoker;
import bg.energo.phoenix.service.billing.billingRun.errors.BillingRunErrorService;
import bg.energo.phoenix.service.billing.billingRun.errors.InvoiceErrorShortObject;
import bg.energo.phoenix.service.billing.invoice.InvoiceEventPublisher;
import bg.energo.phoenix.service.billing.invoice.numberGeneration.InvoiceNumberService;
import bg.energo.phoenix.service.billing.invoice.reversal.InvoiceReversalProcessService;
import bg.energo.phoenix.service.documentMergerService.DocumentMergerService;
import bg.energo.phoenix.service.pod.discount.DiscountService;
import bg.energo.phoenix.service.receivable.CompensationReceivableService;
import bg.energo.phoenix.service.receivable.customerLiability.CustomerLiabilityService;
import bg.energo.phoenix.service.receivable.customerReceivables.CustomerReceivableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static bg.energo.phoenix.model.enums.billing.billings.BillingType.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingRunStartAccountingInvokeService implements BillingRunActionInvoker {
    private final DiscountService discountService;
    private final InvoiceRepository invoiceRepository;
    private final BillingRunProcessHelper processHelper;
    private final TransactionTemplate transactionTemplate;
    private final BillingRunRepository billingRunRepository;
    private final InvoiceEventPublisher invoiceEventPublisher;
    private final DocumentMergerService documentMergerService;
    private final BillingRunErrorService billingRunErrorService;
    private final CustomerLiabilityService customerLiabilityService;
    private final CustomerLiabilityRepository customerLiabilityRepository;
    private final CustomerReceivableService customerReceivableService;
    private final BillingRunEmailSenderService billingRunEmailSenderService;
    private final InvoiceReversalProcessService invoiceReversalProcessService;
    private final InvoiceNumberService invoiceNumberService;
    private final BillingRunInvoicesRepository billingRunInvoicesRepository;
    private final CompensationReceivableService compensationReceivableService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void invoke(BillingRun billingRun) {
        log.debug("Starting billing run process asynchronously");

        Long billingRunId = billingRun.getId();

        ExecutorService executorService = Executors.newFixedThreadPool(5);
        List<InvoiceErrorShortObject> exceptionContext = Collections.synchronizedList(new ArrayList<>());

        try {
            transactionTemplate.executeWithoutResult((x) -> {
                        if (Objects.equals(billingRun.getType(), INVOICE_REVERSAL)) {
                            List<Invoice> reversedInvoices = invoiceReversalProcessService.validateRealGeneration(billingRun);

                            sendAccountingEmails(executorService, billingRun, reversedInvoices, exceptionContext);
                        } else {
                            List<Invoice> allValidInvoicesForAccounting = invoiceRepository.findAllValidInvoicesForAccounting(billingRunId);
                            setInvoiceStatusToReal(allValidInvoicesForAccounting);

                            regenerateRealInvoiceNumbers(new HashSet<>(allValidInvoicesForAccounting));

                            Set<Invoice> validInvoicesForLiabilities = filterInvoicesForLiabilities(allValidInvoicesForAccounting);
                            generateLiabilities(executorService, validInvoicesForLiabilities, exceptionContext);

                            checkDiscounts(allValidInvoicesForAccounting);

                            Set<Invoice> validInvoicesForReceivables = filterInvoicesReceivables(allValidInvoicesForAccounting);
                            generateReceivables(executorService, validInvoicesForReceivables, exceptionContext);

                            sendAccountingEmails(executorService, billingRun, allValidInvoicesForAccounting, exceptionContext);

                            if (billingRun.getType().equals(STANDARD_BILLING)) {
                                createSumDocs(billingRun);
                                deductInterimInvoices(allValidInvoicesForAccounting);
                            }
                            if (billingRun.getType().equals(MANUAL_CREDIT_OR_DEBIT_NOTE)) {
                                makeParentInvoicesDeducted(billingRun.getId());
                            }
                        }
                    }
            );
        } catch (Exception e) {
            transactionTemplate.executeWithoutResult((x) -> {
                addExceptionIntoContext(exceptionContext, "SYSTEM_ERROR", e.getMessage());
                log.error("Exception handled while trying to start accounting");
            });
        } finally {
            finalizeProcess(executorService, billingRun, exceptionContext);
        }
    }

    private void deductInterimInvoices(List<Invoice> allValidInvoicesForAccounting) {
        for (Invoice invoice : allValidInvoicesForAccounting) {
            List<Invoice> interimInvoices = invoiceRepository.findDeductibleInvoicesForRealStandardInvoice(invoice.getId());
            for (Invoice interimInvoice : interimInvoices) {
                interimInvoice.setIsDeducted(true);
                invoiceRepository.save(interimInvoice);
            }
        }
    }

    private void makeParentInvoicesDeducted(Long id) {
        List<Invoice> invoices = billingRunInvoicesRepository.findAllInterimByBillingId(id);
        for (Invoice invoice : invoices) {
            invoice.setIsDeducted(true);
            invoiceRepository.save(invoice);
        }
    }

    @Transactional
    protected void finalizeProcess(ExecutorService executorService,
                                   BillingRun billingRun,
                                   List<InvoiceErrorShortObject> exceptionContext) {
        log.debug("Finalizing billing run process");

        executorService.shutdown();
        log.debug("Waiting for executor to shutdown");

        log.debug("Waiting for executor to finish");
        try {
            boolean finished = executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            log.debug("Executor finished: {}", finished);
        } catch (InterruptedException e) {
            log.error("Exception handled while waiting for executor to finish", e);
        }

        Long billingRunId = billingRun.getId();

        transactionTemplate.executeWithoutResult((x) -> {
            log.debug("Billing run errors published");
            billingRunErrorService.publishBillingErrors(exceptionContext, billingRunId, billingRun.getStatus());
        });
    }

    /**
     * Sends accounting-related emails for all valid invoices asynchronously using
     * the provided ExecutorService.
     *
     * @param executorService               the ExecutorService used to execute email sending tasks asynchronously
     * @param billingRun                    the billing run associated with this batch of invoices
     * @param allValidInvoicesForAccounting the list of valid invoices to send accounting emails for
     * @param exceptionContext              the list to record exception details if any errors occur during email sending
     */
    private void sendAccountingEmails(ExecutorService executorService,
                                      BillingRun billingRun,
                                      List<Invoice> allValidInvoicesForAccounting,
                                      List<InvoiceErrorShortObject> exceptionContext) {
        log.debug("Starting sending accounting emails");
        for (Invoice invoice : allValidInvoicesForAccounting) {
            log.debug("Sending email for invoice with id: {}", invoice.getId());
            executorService.submit(() -> {
                try {
                    billingRunEmailSenderService.createEmailFromInvoiceAndSend(invoice, billingRun, exceptionContext);
                } catch (Exception e) {
                    addExceptionIntoContext(exceptionContext, invoice.getInvoiceNumber(), e.getMessage());
                    log.error("Exception handled while accounting invoice with id: {}", invoice.getId());
                }

                return null;
            });
        }
    }

    /**
     * Generates receivables for a set of valid invoices by processing them in parallel
     * using the provided executor service. Handles exceptions during the processing
     * and adds them to the provided exception context list.
     *
     * @param executorService               the ExecutorService used to process invoices in parallel
     * @param allValidInvoicesForAccounting a set of invoices that are valid for accounting
     * @param exceptionContext              a list to hold exception details for failed invoice processing
     */
    private void generateReceivables(ExecutorService executorService,
                                     Set<Invoice> allValidInvoicesForAccounting,
                                     List<InvoiceErrorShortObject> exceptionContext) {
        log.debug("Starting generating receivables");
        for (Invoice invoice : allValidInvoicesForAccounting) {
            log.debug("Generating receivable for credit note with id: {}", invoice.getId());
            executorService.submit(() -> {
                try {
                    customerReceivableService.createFromInvoice(invoice, LiabilityOrReceivableCreationSource.BILLING_RUN);
                } catch (Exception e) {
                    addExceptionIntoContext(exceptionContext, invoice.getInvoiceNumber(), e.getMessage());
                    log.error("Exception handled while accounting invoice with id: {}", invoice.getId());
                }

                return null;
            });
        }
    }

    /**
     * Processes and checks discounts for a list of valid invoices provided for accounting.
     * Filters invoices based on specific criteria and delegates discount checking
     * to the discount service.
     *
     * @param allValidInvoicesForAccounting the list of valid invoices to be processed for discount checks
     */
    private void checkDiscounts(List<Invoice> allValidInvoicesForAccounting) {
        log.debug("Checking discounts for invoices");
        List<Long> invoicesToCheckDiscount = allValidInvoicesForAccounting
                .stream()
                .filter(invoice -> Objects.equals(invoice.getInvoiceDocumentType(), InvoiceDocumentType.INVOICE)
                        && Objects.equals(invoice.getInvoiceType(), InvoiceType.STANDARD) &&
                        Objects.nonNull(invoice.getBillingId()))
                .map(Invoice::getId)
                .toList();

        discountService.checkInvoicedDiscounts(invoicesToCheckDiscount);
        log.debug("Discounts checked for invoices");
    }

    /**
     * Filters the given list of invoices to exclude credit note invoices and returns a set of invoices
     * that are considered liabilities.
     *
     * @param allValidInvoicesForAccounting the list of all valid invoices available for accounting purposes
     * @return a set of invoices that are liabilities, excluding those with a credit note document type
     */
    private Set<Invoice> filterInvoicesForLiabilities(List<Invoice> allValidInvoicesForAccounting) {
        return allValidInvoicesForAccounting
                .stream()
                .filter(invoice -> !Objects.equals(invoice.getInvoiceDocumentType(), InvoiceDocumentType.CREDIT_NOTE))
                .collect(Collectors.toSet());
    }

    /**
     * Filters the provided list of valid invoices for accounting, returning only those
     * invoices that have an invoice document type of CREDIT_NOTE.
     *
     * @param allValidInvoicesForAccounting the list of invoices to be filtered
     * @return a set of invoices that have an invoice document type of CREDIT_NOTE
     */
    private Set<Invoice> filterInvoicesReceivables(List<Invoice> allValidInvoicesForAccounting) {
        return allValidInvoicesForAccounting
                .stream()
                .filter(invoice -> Objects.equals(invoice.getInvoiceDocumentType(), InvoiceDocumentType.CREDIT_NOTE))
                .collect(Collectors.toSet());
    }

    /**
     * Updates the status of all provided invoices to "REAL" and persists the changes.
     *
     * @param allValidInvoicesForAccounting a list of invoices that need their status set to "REAL"
     */
    private void setInvoiceStatusToReal(List<Invoice> allValidInvoicesForAccounting) {
        log.debug("Setting invoice status to 'REAL' for invoices");
        for (Invoice invoice : allValidInvoicesForAccounting) {
            invoice.setInvoiceStatus(InvoiceStatus.REAL);
            processHelper.updateInvoiceImmediately(invoice);
        }
        log.debug("Invoice status set to 'REAL' for invoices");
    }

    /**
     * Generates liabilities from the provided set of valid invoices by submitting tasks
     * to the provided ExecutorService. Exceptions encountered during liability generation
     * are logged and added to the exception context.
     *
     * @param executorService             the ExecutorService used to execute the liability generation tasks
     * @param validInvoicesForLiabilities the set of invoices from which liabilities need to be generated
     * @param exceptionContext            the list to capture exception details encountered during processing
     */
    private void generateLiabilities(ExecutorService executorService,
                                     Set<Invoice> validInvoicesForLiabilities,
                                     List<InvoiceErrorShortObject> exceptionContext) {
        log.debug("Starting generating liabilities");
        for (Invoice validInvoicesForLiability : validInvoicesForLiabilities) {
            executorService.submit(() -> {
                try {
                    log.debug("Generating liability for invoice with id: {}", validInvoicesForLiability.getId());
                    Long liabilityFromInvoice = customerLiabilityService.createLiabilityFromInvoice(validInvoicesForLiability.getId(), LiabilityOrReceivableCreationSource.BILLING_RUN);
                    compensationReceivableService.runInvoiceCompensation(validInvoicesForLiability, liabilityFromInvoice, true);
                } catch (Exception e) {
                    addExceptionIntoContext(exceptionContext, validInvoicesForLiability.getInvoiceNumber(), e.getMessage());
                    log.error("Exception handled while accounting invoice with id: {}", validInvoicesForLiability.getId());
                    return Boolean.FALSE;
                }
                return Boolean.TRUE;
            });
        }
    }

    /**
     * Regenerates real invoice numbers for the given set of valid invoices for liabilities.
     * This method extracts the IDs of the provided invoices, logs the operation, and publishes
     * an invoice event to regenerate the invoice numbers.
     *
     * @param validInvoicesForLiabilities a set of valid invoices to process for regenerating real invoice numbers
     */
    private void regenerateRealInvoiceNumbers(Set<Invoice> validInvoicesForLiabilities) {
        log.debug("Regenerating real invoice numbers for liabilities");
        Set<Long> liabilityInvoiceIds = validInvoicesForLiabilities
                .stream()
                .map(Invoice::getId)
                .collect(Collectors.toSet());

        invoiceNumberService.fillInvoiceNumber(liabilityInvoiceIds);
        log.debug("Published invoice number regeneration event for liabilities with ids: {}", liabilityInvoiceIds);
    }

    /**
     * Adds an exception information into the provided exception context list.
     *
     * @param exceptionContext the list containing exception information objects
     * @param invoiceNumber    the invoice number associated with the exception
     * @param message          the exception message to be included, trimmed to a maximum of 500 characters
     */
    private void addExceptionIntoContext(List<InvoiceErrorShortObject> exceptionContext,
                                         String invoiceNumber,
                                         String message) {
        exceptionContext.add(new InvoiceErrorShortObject(invoiceNumber, message.substring(0, Math.min(message.length(), 500))));
    }

    /**
     * Creates summary documents for the given billing run.
     *
     * @param billingRun the billing run for which summary documents are to be created.
     *                   This determines the type of invoice processing and document merging.
     */
    private void createSumDocs(BillingRun billingRun) {
        SendingAnInvoice sendingAnInvoice = billingRun.getSendingAnInvoice();
        if (sendingAnInvoice == null) {
            return;
        }
        List<Long> excludedInvoiceIds = new ArrayList<>();
        boolean secondCase = sendingAnInvoice.equals(SendingAnInvoice.ACCORDING_TO_THE_CONTRACT) && billingRunRepository.billingContainsOnPaperFlow(billingRun.getId());
        if (secondCase) {
            excludedInvoiceIds.addAll(invoiceRepository.findInvoiceIdsWithoutBillingGroup(billingRun.getId()));
        }
        if (sendingAnInvoice.equals(SendingAnInvoice.PAPER) || secondCase) {
            documentMergerService.mergeBillingRunDocuments(billingRun, excludedInvoiceIds);
        }
    }
}
