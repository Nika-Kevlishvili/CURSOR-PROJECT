package bg.energo.phoenix.billingRun.service;

import bg.energo.phoenix.billingRun.model.entity.BillingRunContracts;
import bg.energo.phoenix.billingRun.repository.BillingRunBgInvoiceSlotsRepository;
import bg.energo.phoenix.billingRun.repository.BillingRunContractsRepository;
import bg.energo.phoenix.billingRun.repository.BillingRunSvInvoiceSlotsRepository;
import bg.energo.phoenix.model.entity.billing.billingRun.BillingRun;
import bg.energo.phoenix.model.enums.billing.billings.ApplicationModelType;
import bg.energo.phoenix.model.enums.billing.billings.BillingRunMainInvoiceGenerationStatus;
import bg.energo.phoenix.model.enums.billing.billings.BillingStatus;
import bg.energo.phoenix.model.enums.billing.billings.RunStage;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStatus;
import bg.energo.phoenix.repository.billing.billingRun.BillingErrorDataRepository;
import bg.energo.phoenix.repository.billing.billingRun.BillingRunRepository;
import bg.energo.phoenix.service.billing.billingRun.BillingRunProcessHelper;
import bg.energo.phoenix.service.billing.billingRun.actions.startGeneration.BillingRunStartGenerationService;
import bg.energo.phoenix.service.billing.invoice.InvoiceEventPublisher;
import bg.energo.phoenix.service.billing.invoice.InvoiceService;
import bg.energo.phoenix.service.notifications.enums.NotificationState;
import bg.energo.phoenix.service.notifications.enums.NotificationType;
import bg.energo.phoenix.service.notifications.events.NotificationEvent;
import bg.energo.phoenix.service.notifications.service.NotificationEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingRunStandardInvoiceGenerationService {
    private final InvoiceService invoiceService;
    private final BillingRunProcessHelper processHelper;
    private final TransactionTemplate transactionTemplate;
    private final BillingRunRepository billingRunRepository;
    private final InvoiceEventPublisher invoiceEventPublisher;
    private final BillingErrorDataRepository billingErrorDataRepository;
    private final NotificationEventPublisher notificationEventPublisher;
    private final BillingRunContractsRepository billingRunContractsRepository;
    private final BillingRunStandardInvoiceGenerationProcessor generationProcessor;
    private final BillingRunStartGenerationService billingRunStartGenerationService;
    private final BillingRunBgInvoiceSlotsRepository billingRunBgInvoiceSlotsRepository;
    private final BillingRunSvInvoiceSlotsRepository billingRunSvInvoiceSlotsRepository;
    @Value("${app.cfg.billingRun.standard.threadSize}")
    private Integer threadSize;

    @Value("${app.cfg.billingRun.standard.batchSize}")
    private Integer batchSize;

    public void startProcessing(BillingRun billingRun) {
        String schedulerId = UUID.randomUUID().toString();
        log.debug("Scheduler id: {}", schedulerId);

        log.debug("Starting billing run processing {}", billingRun.getId());

        Long billingRunId = billingRun.getId();
        log.debug("Billing run id: {}", billingRunId);

        log.debug("Starting generation");
        ExecutorService executor = Executors.newFixedThreadPool(threadSize);
        log.debug("Executor size: {}", threadSize);

        LocalDateTime billingRunDate = LocalDateTime.now();
        log.debug("Billing run date: {}", billingRunDate);

        billingRun.setMainInvoiceGenerationStatus(BillingRunMainInvoiceGenerationStatus.RUNNING);
        billingRunRepository.save(billingRun);
        log.debug("Setting generation status to running");

        HashSet<Long> failedSlots = new HashSet<>();

        try {
            global:
            while (true) {
                List<BillingRunContracts> billingRunContracts = billingRunContractsRepository
                        .findAllByRunIdAndProcessingStatus(billingRunId, "CREATED", batchSize);
                log.debug("Found {} contracts to process", billingRunContracts.size());

                if (CollectionUtils.isEmpty(billingRunContracts)) {
                    break;
                } else {
                    List<Long> bgInvoiceSlots = billingRunBgInvoiceSlotsRepository.findAllFailedSlots(billingRunId);
                    failedSlots.addAll(bgInvoiceSlots);
                    List<Long> svInvoiceSlots = billingRunSvInvoiceSlotsRepository.findAllFailedSlots(billingRunId);
                    failedSlots.addAll(svInvoiceSlots);

                    for (BillingRunContracts billingRunContract : billingRunContracts) {
                        boolean billingRunStillRunning = isBillingRunStillRunning(billingRun);
                        if (!billingRunStillRunning) {
                            break global;
                        }

                        log.debug("Processing billing run contract with id: {} ", billingRunContract.getId());
                        processSingleContract(billingRun, billingRunContract, executor, schedulerId, billingRunDate, failedSlots);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Exception handled while starting billing run", e);
        } finally {
            log.debug("Finalize process");
            finalizeProcess(executor, billingRun);
        }
    }

    private void finalizeProcess(ExecutorService executor,
                                 BillingRun billingRun) {
        billingRun.setMainInvoiceGenerationStatus(BillingRunMainInvoiceGenerationStatus.FINISHED);
        Long billingRunId = billingRun.getId();

        log.debug("Shutting down executor");
        executor.shutdown();

        log.debug("Waiting for executor to finish");
        try {
            boolean finished = executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            log.debug("Executor finished: {}", finished);
        } catch (InterruptedException e) {
            log.error("Exception handled while waiting for executor to finish", e);
        }

        List<ApplicationModelType> applicationModelType = ListUtils.emptyIfNull(billingRun.getApplicationModelType());
        boolean applicationModelContainsInterimAdvancePayment = applicationModelType.contains(ApplicationModelType.INTERIM_AND_ADVANCE_PAYMENT);

        log.debug("Setting generation status to finished");

        log.debug("Publishing notification for billing run id: {} ", billingRunId);
        publishNotification(billingRunId, NotificationType.BILLING_RUN_ERROR, NotificationState.ERROR);

        if (!applicationModelContainsInterimAdvancePayment) {
            billingRun.setStatus(BillingStatus.DRAFT);
            log.debug("Saving billing run");
            billingRunRepository.save(billingRun);

            billingRunRepository.finalizeDataPreparation(billingRun.getId());

            log.debug("Executing next job in chain for billing run id: {} ", billingRunId);

            invoiceService.generateExcel(billingRunId, InvoiceStatus.DRAFT);

            if (ListUtils.emptyIfNull(billingRun.getRunStages()).contains(RunStage.GENERATE_AND_SIGN)) {
                billingRunStartGenerationService.execute(billingRunId, false, false);
            }
        }
    }

    protected void publishNotification(Long billingRunId, NotificationType notificationType, NotificationState notificationState) {
        if (Objects.equals(NotificationState.ERROR, notificationState)) {
            boolean anyErrorHandledInBillingRun = billingErrorDataRepository.existsByBillingRunId(billingRunId);
            if (anyErrorHandledInBillingRun) {
                notificationEventPublisher.publishNotification(new NotificationEvent(billingRunId, notificationType, billingRunRepository, notificationState));
            }
        } else {
            notificationEventPublisher.publishNotification(new NotificationEvent(billingRunId, notificationType, billingRunRepository, notificationState));
        }
    }

    private void processSingleContract(BillingRun billingRun,
                                       BillingRunContracts nextContract,
                                       ExecutorService executor,
                                       String schedulerId,
                                       LocalDateTime billingRunDate,
                                       HashSet<Long> failedSlots) {
        try {
            nextContract.setProcessingStatus("RUNNING");
            processHelper.updateBillingRunContractsImmediately(nextContract);

            executor.submit(() -> {
                try {
                    MDC.put("schedulerId", schedulerId);
                    MDC.put("billingId", String.valueOf(billingRun.getId()));

                    generationProcessor.processContract(billingRunDate, nextContract, failedSlots, billingRun);
                } catch (Exception e) {
                    log.error("Exception in contract processing, contractId: {}", nextContract.getContractId(), e);
                    nextContract.setProcessingStatus("FAILED");
                } finally {
                    log.debug("Saved run contract");
                    processHelper.updateBillingRunContractsImmediately(nextContract);
                }
            });
        } catch (Exception e) {
            log.error("Exception in contract processing, contractId: {}", nextContract.getContractId(), e);
            nextContract.setProcessingStatus("FAILED");
        } finally {
            log.debug("Saved run contract");
            processHelper.updateBillingRunContractsImmediately(nextContract);
        }
    }

    private boolean isBillingRunStillRunning(BillingRun billingRun) {
        BillingStatus currentStatus = processHelper.getBillingStatus(billingRun);

        if (!Objects.equals(currentStatus, BillingStatus.IN_PROGRESS_DRAFT)) {
            log.debug("Billing run is not in progress draft, stopping generation");
            log.debug("Billing status: {}", currentStatus);

            return false;
        }

        return true;
    }
}
