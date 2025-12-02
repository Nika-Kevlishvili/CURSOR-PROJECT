package bg.energo.phoenix.service.archivation.edms;

import bg.energo.phoenix.repository.contract.action.ActionFileRepository;
import bg.energo.phoenix.repository.contract.activity.SystemActivityFileRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractDocumentRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractFileRepository;
import bg.energo.phoenix.repository.contract.service.ServiceContractAdditionalDocumentsRepository;
import bg.energo.phoenix.repository.contract.service.ServiceContractFilesRepository;
import bg.energo.phoenix.repository.crm.emailCommunication.EmailCommunicationFileRepository;
import bg.energo.phoenix.repository.crm.smsCommunication.SmsCommunicationFilesRepository;
import bg.energo.phoenix.repository.documents.DocumentsRepository;
import bg.energo.phoenix.repository.product.product.ProductFileRepository;
import bg.energo.phoenix.repository.product.service.ServiceFileRepository;
import bg.energo.phoenix.repository.receivable.cancellationOfDisconnectionOfThePowerSupply.PowerSupplyDcnCancellationFileRepository;
import bg.energo.phoenix.repository.receivable.customerAssessment.CustomerAssessmentFilesRepository;
import bg.energo.phoenix.repository.receivable.reconnectionOfPowerSupply.ReconnectionOfThePowerSupplyFilesRepository;
import bg.energo.phoenix.repository.receivable.rescheduling.ReschedulingFilesRepository;
import bg.energo.phoenix.util.archivation.EDMSArchivationConstraints;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveFilesCleanerAndRetryingService {
    private final ArchiveFilesCleanerProcessor archiveFilesCleanerProcessor;
    private final RetryingFailedArchivationProcessor retryingFailedArchivationProcessor;
    private final ProductFileRepository productFileRepository;
    private final ServiceFileRepository serviceFileRepository;
    private final ProductContractFileRepository productContractFileRepository;
    private final ProductContractDocumentRepository productContractDocumentRepository;
    private final ServiceContractFilesRepository serviceContractFilesRepository;
    private final ServiceContractAdditionalDocumentsRepository serviceContractAdditionalDocumentsRepository;
    private final ActionFileRepository actionFileRepository;
    private final CustomerAssessmentFilesRepository customerAssessmentFilesRepository;
    private final ReschedulingFilesRepository reschedulingFilesRepository;
    private final PowerSupplyDcnCancellationFileRepository cancellationFileRepository;
    private final ReconnectionOfThePowerSupplyFilesRepository reconnectionFileRepository;
    private final SystemActivityFileRepository systemActivityFileRepository;
    private final SmsCommunicationFilesRepository smsCommunicationFilesRepository;
    private final EmailCommunicationFileRepository emailCommunicationFileRepository;
    private final DocumentsRepository documentsRepository;

    @Async
    public void cleanupArchivedFiles() {
        ExecutorService executorService = Executors.newFixedThreadPool(5);

        archiveFilesCleanerProcessor.deleteFiles(executorService, productFileRepository);
        archiveFilesCleanerProcessor.deleteFiles(executorService, serviceFileRepository);
        archiveFilesCleanerProcessor.deleteFiles(executorService, productContractFileRepository);
        archiveFilesCleanerProcessor.deleteFiles(executorService, productContractDocumentRepository);
        archiveFilesCleanerProcessor.deleteFiles(executorService, serviceContractFilesRepository);
        archiveFilesCleanerProcessor.deleteFiles(executorService, serviceContractAdditionalDocumentsRepository);
        archiveFilesCleanerProcessor.deleteFiles(executorService, actionFileRepository);
        archiveFilesCleanerProcessor.deleteFiles(executorService, customerAssessmentFilesRepository);
        archiveFilesCleanerProcessor.deleteFiles(executorService, reschedulingFilesRepository);
        archiveFilesCleanerProcessor.deleteFiles(executorService, cancellationFileRepository);
        archiveFilesCleanerProcessor.deleteFiles(executorService, reconnectionFileRepository);
        archiveFilesCleanerProcessor.deleteFiles(executorService, systemActivityFileRepository);
        archiveFilesCleanerProcessor.deleteFiles(executorService, smsCommunicationFilesRepository);
        archiveFilesCleanerProcessor.deleteFiles(executorService, emailCommunicationFileRepository);
        archiveFilesCleanerProcessor.deleteFiles(executorService, documentsRepository);

        executorService.shutdown();
    }

    @Async
    public void retryArchivingFiles() {
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        retryingFailedArchivationProcessor.retryArchivation(executorService, productFileRepository, EDMSArchivationConstraints.DOCUMENT_TYPE_PRODUCT_FILE);
        retryingFailedArchivationProcessor.retryArchivation(executorService, serviceFileRepository, EDMSArchivationConstraints.DOCUMENT_TYPE_SERVICE_FILE);
        retryingFailedArchivationProcessor.retryArchivation(executorService, productContractFileRepository, EDMSArchivationConstraints.DOCUMENT_TYPE_PRODUCT_CONTRACT_FILE);
        retryingFailedArchivationProcessor.retryArchivation(executorService, productContractDocumentRepository, EDMSArchivationConstraints.DOCUMENT_TYPE_PRODUCT_CONTRACT_DOCUMENT);
        retryingFailedArchivationProcessor.retryArchivation(executorService, serviceContractFilesRepository, EDMSArchivationConstraints.DOCUMENT_TYPE_SERVICE_CONTRACT_FILE);
        retryingFailedArchivationProcessor.retryArchivation(executorService, serviceContractAdditionalDocumentsRepository, EDMSArchivationConstraints.DOCUMENT_TYPE_SERVICE_CONTRACT_DOCUMENT);
        retryingFailedArchivationProcessor.retryArchivation(executorService, actionFileRepository, EDMSArchivationConstraints.DOCUMENT_TYPE_ACTION_FILE);
        retryingFailedArchivationProcessor.retryArchivation(executorService, customerAssessmentFilesRepository, EDMSArchivationConstraints.DOCUMENT_TYPE_CUSTOMER_ASSESSMENT_FILE);
        retryingFailedArchivationProcessor.retryArchivation(executorService, reschedulingFilesRepository, EDMSArchivationConstraints.DOCUMENT_TYPE_RESCHEDULING_FILE);
        retryingFailedArchivationProcessor.retryArchivation(executorService, cancellationFileRepository, EDMSArchivationConstraints.DOCUMENT_TYPE_CANCELLATION_OF_DISCONNECTION_FILE);
        retryingFailedArchivationProcessor.retryArchivation(executorService, reconnectionFileRepository, EDMSArchivationConstraints.DOCUMENT_TYPE_RECONNECTION_OF_THE_POWER_SUPPLY_FILE);
        retryingFailedArchivationProcessor.retryArchivation(executorService, systemActivityFileRepository, EDMSArchivationConstraints.DOCUMENT_TYPE_SYSTEM_ACTIVITY_FILE);
        retryingFailedArchivationProcessor.retryArchivation(executorService, smsCommunicationFilesRepository, EDMSArchivationConstraints.DOCUMENT_TYPE_SMS_COMMUNICATION_FILE);
        retryingFailedArchivationProcessor.retryArchivation(executorService, emailCommunicationFileRepository, EDMSArchivationConstraints.DOCUMENT_TYPE_EMAIL_FILE);
        retryingFailedArchivationProcessor.retryArchivation(executorService, documentsRepository, null);
        executorService.shutdown();
        log.info("Archiving files retrying job completed.");
    }

}
