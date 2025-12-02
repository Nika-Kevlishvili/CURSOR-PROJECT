package bg.energo.phoenix.service.billing.billingRun.actions.startAccounting;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.entity.billing.billingRun.BillingRun;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.enums.billing.billings.BillingPermissions;
import bg.energo.phoenix.model.enums.billing.billings.BillingRunProcessStage;
import bg.energo.phoenix.model.enums.billing.billings.BillingStatus;
import bg.energo.phoenix.model.enums.billing.billings.BillingType;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStatus;
import bg.energo.phoenix.repository.billing.billingRun.BillingErrorDataRepository;
import bg.energo.phoenix.repository.billing.billingRun.BillingRunRepository;
import bg.energo.phoenix.repository.documents.DocumentsRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.billing.billingRun.BillingRunOutdatedDocumentService;
import bg.energo.phoenix.service.billing.billingRun.actions.AbstractBillingRunActionService;
import bg.energo.phoenix.service.billing.billingRun.errors.BillingRunErrorService;
import bg.energo.phoenix.service.billing.billingRun.errors.InvoiceErrorShortObject;
import bg.energo.phoenix.service.billing.invoice.InvoiceService;
import bg.energo.phoenix.service.notifications.enums.NotificationState;
import bg.energo.phoenix.service.notifications.enums.NotificationType;
import bg.energo.phoenix.service.notifications.events.NotificationEvent;
import bg.energo.phoenix.service.notifications.service.NotificationEventPublisher;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.sql.CallableStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class BillingRunStartAccountingService extends AbstractBillingRunActionService {
    private final BillingRunStartAccountingInvokeService billingRunStartAccountingInvokeService;
    private final InvoiceService invoiceService;
    private final BillingRunErrorService billingRunErrorService;
    private final BillingRunOutdatedDocumentService billingRunOutdatedDocumentService;
    private final DocumentsRepository documentRepository;

    @PersistenceContext
    private final EntityManager entityManager;

    public BillingRunStartAccountingService(PermissionService permissionService,
                                            BillingRunStartAccountingInvokeService billingRunStartAccountingInvokeService,
                                            BillingRunRepository billingRunRepository,
                                            NotificationEventPublisher notificationEventPublisher,
                                            BillingErrorDataRepository billingErrorDataRepository,
                                            InvoiceService invoiceService,
                                            BillingRunErrorService billingRunErrorService,
                                            BillingRunOutdatedDocumentService billingRunOutdatedDocumentService,
                                            DocumentsRepository documentRepository,
                                            EntityManager entityManager) {
        super(permissionService, billingRunRepository, notificationEventPublisher, billingErrorDataRepository);
        this.billingRunStartAccountingInvokeService = billingRunStartAccountingInvokeService;
        this.invoiceService = invoiceService;
        this.billingRunErrorService = billingRunErrorService;
        this.billingRunOutdatedDocumentService = billingRunOutdatedDocumentService;
        this.documentRepository = documentRepository;
        this.entityManager = entityManager;
    }

    @Override
    public AbstractBillingRunActionService getNextJobInChain() {
        return null;
    }

    /**
     * Starts the accounting process for a billing run.
     *
     * @param billingRunId the ID of the billing run to start accounting for
     */
    public void execute(Long billingRunId, boolean isResumeProcess, boolean mustCheckPermission) {
        BillingRun billingRun = billingRunRepository
                .findById(billingRunId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Billing run does not exists with id: [%s]".formatted(billingRunId)));
        if (mustCheckPermission && missingPermission(BillingPermissions.START_ACCOUNTING, billingRun.getType())) {
            log.error("Can't start accounting for billing run without permission");
            throw new ClientException("Can't start accounting for billing run without permission;", ErrorCode.OPERATION_NOT_ALLOWED);
        }

        if (!isResumeProcess) {
            if (!Objects.equals(billingRun.getStatus(), BillingStatus.GENERATED)) {
                log.error("Billing run must be in status 'GENERATED' to start accounting process");
                throw new ClientException("Billing run must be in status 'GENERATED' to start accounting process", ErrorCode.OPERATION_NOT_ALLOWED);
            }
        }

        if (Objects.equals(billingRun.getType(), BillingType.MANUAL_CREDIT_OR_DEBIT_NOTE) &&
                (billingRunRepository.hasAnyCancelledInvoiceConnected(billingRunId))) {
            log.error("This invoice can’t become real, connected invoice is already cancelled");
            throw new ClientException("This invoice can’t become real, connected invoice is already cancelled", ErrorCode.CONFLICT);
        }

        log.debug("Setting billing run status to 'IN_PROGRESS_ACCOUNTING'");
        billingRun.setProcessStage(BillingRunProcessStage.ACCOUNTING);
        billingRun.setStatus(BillingStatus.IN_PROGRESS_ACCOUNTING);

        billingRunRepository.save(billingRun);
        log.debug("Setting billing run status to 'IN_PROGRESS_ACCOUNTING'");

        new Thread(() -> {
            try {
                billingRunStartAccountingInvokeService.invoke(billingRun);

                log.debug("Billing run status changed to 'COMPLETED'");
                billingRun.setStatus(BillingStatus.COMPLETED);
                billingRunRepository.save(billingRun);

                tryToGenerateExcelReportFile(billingRunId);

                Session session = entityManager.unwrap(Session.class);
                session.doWork((work) -> {
                    Long runId = billingRun.getId();
                    MDC.put("billingId", String.valueOf(runId));
                    log.debug("Starting finalization billing run with id: [%s]".formatted(runId));
                    CallableStatement statement = work.prepareCall("CALL billing_run.make_billing_run_real(?)");
                    log.debug("Procedure call was successful for finalization billing run;");
                    statement.setLong(1, runId);
                    statement.execute();

                    MDC.popByKey("billingId");
                });

            } catch (Exception e) {
                billingRun.setStatus(BillingStatus.GENERATED);
                List<InvoiceErrorShortObject> exceptionContext = new ArrayList<>();
                exceptionContext.add(new InvoiceErrorShortObject("GLOBAL", e.getMessage()));
                billingRunErrorService.publishBillingErrors(exceptionContext, billingRunId, billingRun.getStatus());
            }
            List<Document> billingRunInvoiceDocumentsIsNotReal = documentRepository
                    .findBillingRunInvoiceDocumentsIsNotReal(billingRunId);

            billingRunOutdatedDocumentService.deleteOutdatedDocuments(billingRunInvoiceDocumentsIsNotReal);

            log.debug("Publishing notification for billing run with id: {}", billingRunId);
            notificationEventPublisher.publishNotification(new NotificationEvent(billingRunId, NotificationType.BILLING_RUN_COMPLETE, billingRunRepository, NotificationState.COMPLETE));
        }).start();
    }

    private void tryToGenerateExcelReportFile(Long billingRunId) {
        try {
            log.debug("Generating excel for billing run with id: {}", billingRunId);
            invoiceService.generateExcel(billingRunId, InvoiceStatus.REAL);
            log.debug("Excel generated for billing run with id: {}", billingRunId);
        } catch (Exception e) {
            log.error("Exception handled while trying to generate excel for billing run with id: {}", billingRunId);
        }
    }
}
