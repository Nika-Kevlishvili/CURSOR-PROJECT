package bg.energo.phoenix.service.massImport;

import bg.energo.phoenix.event.EventType;
import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.entity.receivable.collectionChannel.CollectionChannel;
import bg.energo.phoenix.model.request.receivable.payment.CreatePaymentRequest;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.process.repository.ProcessRepository;
import bg.energo.phoenix.process.repository.ProcessedRecordInfoRepository;
import bg.energo.phoenix.repository.receivable.collectionChannel.CollectionChannelRepository;
import bg.energo.phoenix.repository.receivable.paymentPackage.PaymentPackageRepository;
import bg.energo.phoenix.repository.receivable.paymentPackage.PaymentPackageStatusChangeHistoryRepository;
import bg.energo.phoenix.service.notifications.service.NotificationEventPublisher;
import bg.energo.phoenix.service.receivable.payment.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The TxtTestMassImportProcessService class is responsible for processing text-based test mass import files.
 * It extends the AbstractTxtMassImportProcessService class and provides the implementation for processing
 * individual records from the import file.
 */
@Service
@Slf4j
public class PaymentMassImportProcessService extends AbstractTxtMassImportProcessService {


    private final PaymentPartnerMapper paymentPartnerMapper;
    private final BankPartnerMapper bankPartnerMapper;
    private final PaymentService paymentService;
    @Value("${app.cfg.txtTest.massImport.numberOfRowsPerTask}")
    private Integer numberOfRowsPerTsk;
    @Value("${app.cfg.txtTest.massImport.numberOfTasksPerThread}")
    private Integer numberOfCallablesPerThread;
    @Value("${app.cfg.txtTest.massImport.numberOfThreads}")
    private Integer numberOfThreads;


    public PaymentMassImportProcessService(PaymentPartnerMapper paymentPartnerMapper,
                                           PaymentService paymentService,
                                           ProcessRepository processRepository,
                                           ProcessedRecordInfoRepository processedRecordInfoRepository,
                                           CollectionChannelRepository collectionChannelRepository,
                                           PaymentPackageRepository paymentPackageRepository,
                                           BankPartnerMapper bankPartnerMapper,
                                           NotificationEventPublisher notificationEventPublisher, PaymentPackageStatusChangeHistoryRepository paymentPackageStatusChangeHistoryRepository,
                                           TransactionTemplate transactionTemplate) {
        super(notificationEventPublisher, processRepository, processedRecordInfoRepository, paymentPackageRepository, collectionChannelRepository, paymentPackageStatusChangeHistoryRepository, transactionTemplate);
        this.paymentPartnerMapper = paymentPartnerMapper;
        this.paymentService = paymentService;
        this.bankPartnerMapper = bankPartnerMapper;
    }

    /**
     * Processes an individual record from the import file.
     *
     * @param recordData          the record data as a map
     * @param permissions         the set of permissions for the current user
     * @param processSysUserId    the system user ID of the user running the process
     * @param date                the date associated with the process
     * @param processRecordInfo   the ID of the processed record info
     * @param collectionChannelId the ID of the collection channel
     * @param isBankFile          a flag indicating whether the file is a bank file
     * @param paymentPackageId    the ID of the payment package
     * @return a string representing the result of processing the record
     */
    @Override
    protected String processRecord(
            Map<String, String> recordData,
            Set<String> permissions,
            String processSysUserId,
            LocalDate date,
            Long processRecordInfo,
            Long collectionChannelId,
            boolean isBankFile,
            Long paymentPackageId,
            Map<String, String> bankFileMetadata,
            Boolean currencyFromCollectionChannel,
            CollectionChannel collectionChannel
    ) {
        List<String> errorMessages = new ArrayList<>();
        if (SecurityContextHolder.getContext().getAuthentication() != null && !permissions.contains(PermissionEnum.PAYMENT_MI_CREATE.getId())) {
            throw new ClientException("Not enough permission for creating payment", ErrorCode.ACCESS_DENIED);
        }
        if (!isBankFile) {
            PaymentPartnerRecord paymentPartnerRecord = paymentPartnerMapper.mapToPaymentPartnerRecord(recordData, errorMessages);
            paymentPartnerRecord.setCollectionChannelId(collectionChannelId);
            CreatePaymentRequest paymentRequest = paymentPartnerMapper.mapToPaymentRequest(
                    paymentPartnerRecord,
                    date,
                    errorMessages,
                    paymentPackageId,
                    currencyFromCollectionChannel,
                    collectionChannel
            );
            validateRequest(errorMessages, paymentRequest);
            return String.valueOf(paymentService.create(paymentRequest, permissions));
        } else {
            BankPartnerRecord bankPartnerRecord = bankPartnerMapper.mapToBankPartnerRecord(
                    recordData,
                    bankFileMetadata.get("accountIdentification"),
                    bankFileMetadata.get("currency"),
                    errorMessages
            );
            bankPartnerRecord.setCollectionChannelId(collectionChannelId);
            CreatePaymentRequest paymentRequest = bankPartnerMapper.mapToPaymentRequest(
                    bankPartnerRecord,
                    date,
                    errorMessages,
                    paymentPackageId
            );
            validateRequest(errorMessages, paymentRequest);
            return String.valueOf(paymentService.create(paymentRequest, permissions));
        }
    }

    @Override
    protected boolean isBankFile(String txtFile) {
        return txtFile.startsWith(":25:");
    }

    @Override
    protected Map<String, String> parseBankFileMetadata(String txtFile, List<String> errorMessages) {
        return bankPartnerMapper.parseMetadata(txtFile, errorMessages);
    }

    @Override
    protected List<Integer> findBankFileRecordDelimiters(String txtFile) {
        return bankPartnerMapper.findRecordDelimiters(txtFile);
    }

    @Override
    protected List<Map<String, String>> parseBankFile(String txtFile, List<Integer> recordDelimiters, List<String> errorMessages) {
        return bankPartnerMapper.parseBankPartnerFile(txtFile, recordDelimiters, errorMessages);
    }

    @Override
    protected List<Map<String, String>> parsePaymentPartnerFile(String txtFile) {
        return paymentPartnerMapper.parsePaymentPartnerFile(txtFile);
    }

    @Override
    protected String getIdentifier(Map<String, String> recordData, boolean isBankFile) {
        if (!isBankFile) {
            return recordData.get("customerNumber");
        } else {
            String field61 = recordData.get("61");
            if (field61 == null || !field61.contains("N")) {
                return "INVALID_61_FIELD";
            }
            String[] parts = field61.split("N");
            if (parts.length < 2) {
                return "INVALID_61_FIELD";
            }
            return parts[1].trim();
        }
    }

    @Override
    protected int getNumberOfThreads() {
        return numberOfThreads;
    }

    @Override
    protected int getNumberOfCallablesPerThread() {
        return numberOfCallablesPerThread;
    }

    @Override
    protected int getNumberOfRowsPerTask() {
        return numberOfRowsPerTsk;
    }

    @Override
    public boolean supports(EventType eventType) {
        return EventType.PAYMENT_MASS_IMPORT_PROCESS.equals(eventType);
    }
}