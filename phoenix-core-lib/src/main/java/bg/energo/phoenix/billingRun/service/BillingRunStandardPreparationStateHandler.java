package bg.energo.phoenix.billingRun.service;

import bg.energo.phoenix.model.entity.billing.billingRun.BillingRun;
import bg.energo.phoenix.model.enums.billing.billings.*;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStatus;
import bg.energo.phoenix.repository.billing.billingRun.BillingRunRepository;
import bg.energo.phoenix.service.billing.billingRun.BillingRunProcessHelper;
import bg.energo.phoenix.service.billing.invoice.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingRunStandardPreparationStateHandler {
    private final InvoiceService invoiceService;
    private final BillingRunProcessHelper processHelper;
    private final BillingRunRepository billingRunRepository;
    private final BillingRunStandardPreparationService preparationService;
    private final BillingRunStandardInvoiceGenerationProcessor generationProcessor;
    private final BillingRunStandardInvoiceGenerationService invoiceGenerationService;

    public void startStandardBillingProcessing() {
        List<BillingRun> billingRuns = billingRunRepository.findBillingRunsWithPreparedStandardBillingRunData();
        if (CollectionUtils.isNotEmpty(billingRuns)) {
            String schedulerId = UUID.randomUUID().toString();
            MDC.put("schedulerId", schedulerId);

            log.debug("Started scheduler for billing data generation and found data {}", billingRuns.size());
            for (BillingRun billingRun : billingRuns) {
                MDC.put("billingId", String.valueOf(billingRun.getId()));

                BillingRunMainInvoiceGenerationStatus mainInvoiceGenerationStatus = billingRun.getMainInvoiceGenerationStatus();
                boolean isBillingRunDataGenerationStarted = Objects.nonNull(mainInvoiceGenerationStatus);

                switch (billingRun.getType()) {
                    case STANDARD_BILLING -> {
                        List<ApplicationModelType> applicationModelType = billingRun.getApplicationModelType();
                        if (applicationModelType == null) {
                            log.error("Billing run has not application model type, cannot start billing");
                            continue;
                        }

                        // run invoice generation for standard billing application models
                        List<ApplicationModelType> standardBillingApplicationModels = List.of(
                                ApplicationModelType.FOR_VOLUMES,
                                ApplicationModelType.OVER_TIME_PERIODICAL,
                                ApplicationModelType.OVER_TIME_ONE_TIME,
                                ApplicationModelType.PER_PIECE,
                                ApplicationModelType.WITH_ELECTRICITY_INVOICE
                        );
                        boolean applicationModelContainsStandardBilling = applicationModelType.stream().anyMatch(standardBillingApplicationModels::contains);
                        boolean applicationModelContainsInterimAdvancePayment = applicationModelType.contains(ApplicationModelType.INTERIM_AND_ADVANCE_PAYMENT);

                        if (applicationModelContainsStandardBilling && applicationModelContainsInterimAdvancePayment) {
                            log.debug("Both Interim and standard selected;");
                            if (!isBillingRunDataGenerationStarted) {
                                invoiceGenerationService.startProcessing(billingRun);
                            } else {
                                boolean isBillingRunDataGenerationFinished = Objects.equals(mainInvoiceGenerationStatus, BillingRunMainInvoiceGenerationStatus.FINISHED);

                                if (isBillingRunDataGenerationFinished) {
                                    BillingRunDataPreparationStatus interimDataPreparationStatus = billingRun.getInterimDataPreparationStatus();
                                    boolean isInterimDataPreparationStarted = Objects.nonNull(interimDataPreparationStatus);

                                    if (!isInterimDataPreparationStarted) {
                                        preparationService.startInterimAdvancePaymentPreparation(billingRun);
                                        billingRun.setInterimDataPreparationStatus(BillingRunDataPreparationStatus.RUNNING);
                                    } else {
                                        boolean isInterimDataPreparationFinished = Objects.equals(interimDataPreparationStatus, BillingRunDataPreparationStatus.FINISHED);

                                        if (isInterimDataPreparationFinished) {
                                            generationProcessor.startInterimProcessing(billingRun, schedulerId);
                                        }
                                    }
                                }
                            }
                        } else if (applicationModelContainsInterimAdvancePayment) {
                            log.debug("Only interim is selected!;");
                            BillingRunDataPreparationStatus interimDataPreparationStatus = billingRun.getInterimDataPreparationStatus();
                            boolean isInterimDataPreparationStarted = Objects.nonNull(interimDataPreparationStatus);
                            if (isInterimDataPreparationStarted) {
                                log.debug("Interim preparation is started!;");
                                boolean isInterimDataPreparationFinished = Objects.equals(interimDataPreparationStatus, BillingRunDataPreparationStatus.FINISHED);
                                if (isInterimDataPreparationFinished) {
                                    log.debug("Interim preparation is finished!;");
                                    generationProcessor.startInterimProcessing(billingRun, schedulerId);
                                }
                            } else {
                                log.debug("Interim preparation was not started!; Starting ...");
                                preparationService.startInterimAdvancePaymentPreparation(billingRun);
                                billingRun.setInterimDataPreparationStatus(BillingRunDataPreparationStatus.RUNNING);
                            }
                        } else if (applicationModelContainsStandardBilling || billingRun.getType().equals(BillingType.INVOICE_CORRECTION)) {
                            if (!isBillingRunDataGenerationStarted) {
                                invoiceGenerationService.startProcessing(billingRun);
                            }
                        }
                    }
                    case INVOICE_CORRECTION -> {
                        if (!isBillingRunDataGenerationStarted) {
                            invoiceGenerationService.startProcessing(billingRun);
                        }
                    }
                }

                billingRunRepository.save(billingRun);
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finishStandardBillingProcessing() {
        List<BillingRun> standardBillingRunWithFinishedBillingData = billingRunRepository.findStandardBillingRunWithFinishedBillingData();
        if (CollectionUtils.isNotEmpty(standardBillingRunWithFinishedBillingData)) {
            for (BillingRun billingRun : standardBillingRunWithFinishedBillingData) {
                try {
                    billingRun.setMainInvoiceGenerationStatus(BillingRunMainInvoiceGenerationStatus.FINISHED);
                    billingRun.setStatus(BillingStatus.DRAFT);

                    processHelper.updateBillingRunImmediately(billingRun);

                    invoiceService.generateExcel(billingRun.getId(), InvoiceStatus.DRAFT);
                } catch (Exception e) {
                    log.error("Exception handled while finishing billing run start billing process", e);
                }
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void resumeNonFinishedProcesses() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        try {
            List<BillingRun> nonFinishedProcesses = billingRunRepository.findNonFinishedProcesses();

            for (BillingRun nonFinishedProcess : nonFinishedProcesses) {
                executorService.submit(() -> {
                    // billing run invoice date equals or after current date
                    boolean canBillingRunCanBeResumed = Objects.nonNull(nonFinishedProcess.getInvoiceDate()) && !nonFinishedProcess.getInvoiceDate().isBefore(LocalDate.now());
                    if (canBillingRunCanBeResumed) {
                        log.debug("Resuming billing run with id: {}", nonFinishedProcess.getId());
                        boolean isMainInvoiceGenerationRunning = Objects.equals(nonFinishedProcess.getMainInvoiceGenerationStatus(), BillingRunMainInvoiceGenerationStatus.RUNNING);
                        if (isMainInvoiceGenerationRunning) {
                            log.debug("Running stage is main invoice generation, resuming process");
                            invoiceGenerationService.startProcessing(nonFinishedProcess);
                        } else {
                            boolean isInterimAdvancePaymentGenerationRunning = Objects.equals(nonFinishedProcess.getInterimInvoiceGenerationStatus(), BillingRunDataPreparationStatus.RUNNING);
                            if (isInterimAdvancePaymentGenerationRunning) {
                                String schedulerId = UUID.randomUUID().toString();
                                log.debug("Running stage is interim invoice generation, resuming process");
                                generationProcessor.startInterimProcessing(nonFinishedProcess, schedulerId);
                            }
                        }
                    }
                });

                executorService.shutdown();
            }
        } catch (Exception e) {
            log.error("Exception handled while resuming non finished processes", e);
        } finally {
            try {
                boolean terminationCompleted = executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

                if (!terminationCompleted) {
                    log.error("Executor did not terminate in time");
                }
            } catch (Exception e) {
                log.error("Exception handled while trying to finish non finished processes", e);
            }
        }
    }
}
