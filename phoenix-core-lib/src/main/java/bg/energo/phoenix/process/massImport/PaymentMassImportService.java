package bg.energo.phoenix.process.massImport;

import bg.energo.phoenix.event.EventType;
import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.collectionChannel.CollectionChannel;
import bg.energo.phoenix.model.enums.receivable.collectionChannel.CollectionChannelType;
import bg.energo.phoenix.model.enums.receivable.paymentPackage.PaymentPackageLockStatus;
import bg.energo.phoenix.model.enums.receivable.paymentPackage.PaymentPackageType;
import bg.energo.phoenix.model.request.receivable.paymentPackage.PaymentPackageCreateRequest;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.process.model.entity.Process;
import bg.energo.phoenix.process.model.enums.ProcessType;
import bg.energo.phoenix.process.repository.ProcessRepository;
import bg.energo.phoenix.repository.billing.accountingPeriods.AccountingPeriodsRepository;
import bg.energo.phoenix.repository.receivable.collectionChannel.CollectionChannelRepository;
import bg.energo.phoenix.repository.receivable.paymentPackage.PaymentPackageRepository;
import bg.energo.phoenix.service.massImport.BankPartnerMapper;
import bg.energo.phoenix.service.massImport.PaymentPartnerMapper;
import bg.energo.phoenix.service.receivable.paymentPackage.PaymentPackageService;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.mozilla.universalchardet.UniversalDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static bg.energo.phoenix.exception.ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED;
import static bg.energo.phoenix.process.model.enums.ProcessStatus.NOT_STARTED;

@Service
@RequiredArgsConstructor
public class PaymentMassImportService extends MassImportBaseService {

    private static final Logger log = LoggerFactory.getLogger(PaymentMassImportService.class);
    private final CollectionChannelRepository collectionChannelRepository;
    private final BankPartnerMapper bankPartnerMapper;
    private final PaymentPartnerMapper paymentPartnerMapper;
    private final AccountingPeriodsRepository accountingPeriodsRepository;
    private final PaymentPackageService paymentPackageService;
    private final ProcessRepository processRepository;
    private final PaymentPackageRepository paymentPackageRepository;

    @Override
    public DomainType getDomainType() {
        return DomainType.PAYMENT;
    }

    @Override
    protected EventType getEventType() {
        return EventType.PAYMENT_MASS_IMPORT_PROCESS;
    }

    @Override
    protected ProcessType getProcessType() {
        return ProcessType.PROCESS_PAYMENT_MASS_IMPORT;
    }

    @Override
    protected String getFileUploadPath() {
        return "/payment_mass_import";
    }

    @Override
    protected PermissionContextEnum getPermissionContext() {
        return PermissionContextEnum.PAYMENT_MI;
    }

    @Override
    public void uploadMassImportFile(MultipartFile file, LocalDate date, Long collectionChannelId, Boolean currencyFromCollectionChannel) {
        log.debug("starting mass import for collection channel with id {}", collectionChannelId);
        validateFileStructure(file);
        log.debug("file structure validated for collection channel with id {}", collectionChannelId);

        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("text/plain")) {
            log.debug("Invalid file format for collection channel with id {}", collectionChannelId);
            throw new ClientException("Invalid file format", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        CollectionChannel collectionChannel = collectionChannelRepository
                .findByIdAndStatuses(collectionChannelId, List.of(EntityStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("Collection channel not found!;"));

        if (collectionChannel.getType().equals(CollectionChannelType.ONLINE)) {
            log.debug("Collection channel must be offline , collection channel ID : {}", collectionChannelId);
            throw new ClientException("Collection channel must be offline!;", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (!checkProvidedProcessDate(date)) {
            log.debug("Invalid date provided! for collection channel with id {}", collectionChannelId);
            throw new ClientException("Invalid date provided!;", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        String remotePath = ftpBasePath + getFileUploadPath() + "/" + LocalDate.now();
        String fileUrl = fileService.uploadFile(file, remotePath, UUID.randomUUID() + "_" + file.getOriginalFilename());
        List<String> permissions = permissionService.getPermissionsFromContext(getPermissionContext());
        Long paymentPackageId = createAndFetchPaymentPackage(date, collectionChannelId);
        Process process;
        try {
            process = processService.createProcess(
                    getProcessType(),
                    NOT_STARTED,
                    fileUrl,
                    String.join(";", permissions),
                    date,
                    collectionChannelId,
                    paymentPackageId,
                    currencyFromCollectionChannel
            );
        } catch (Exception e) {
            paymentPackageRepository.deleteById(paymentPackageId);
            log.debug("Error during process creation!");
            log.debug("Error message : {},", e.getMessage());
            throw new ClientException("Error during process creation", ErrorCode.APPLICATION_ERROR);
        }
        log.debug("process created with id {}, for collection channel {} ", process.getId(), collectionChannelId);
//        applicationEventPublisher.publishEvent(getProcessCreatedEvent(process));
        rabbitMQProducerService.publishProcessEvent(eventFactory.createProcessCreatedEvent(
                getEventType(),
                process
        ));
    }



    private boolean checkProvidedProcessDate(LocalDate date) {
        LocalDate startDate = LocalDate.of(1990, 1, 1);
        LocalDate endDate = LocalDate.of(2090, 12, 31);

        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    @SneakyThrows
    private void validateFileStructure(MultipartFile file) {
        ByteArrayResource byteArrayResource;
        try {
            byte[] bytes = file.getBytes();
            byteArrayResource = new ByteArrayResource(bytes);
        } catch (IOException e) {
            throw new ClientException("Error reading file content", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        byte[] bytes = byteArrayResource.getByteArray();
        String encoding = detectEncoding(bytes);
        String txtFile = new String(bytes, encoding);
        boolean isBankFile = isBankFile(txtFile);
        List<String> errorMessages = new ArrayList<>();

        if (isBankFile) {
            try {
                bankPartnerMapper.parseMetadata(txtFile, errorMessages);
                bankPartnerMapper.validateBankPartnerFileStructure(txtFile, errorMessages);
            } catch (Exception e) {
                log.debug("Exception caught while parsing bank partner file structure , message : {}", e.getMessage());
                throw new ClientException("Incorrect file structure provided;", ILLEGAL_ARGUMENTS_PROVIDED);
            }
        } else {
            try {
                paymentPartnerMapper.parsePaymentPartnerFile(txtFile);
            } catch (Exception e) {
                log.debug("Exception caught while parsing payment partner file structure , message : {}", e.getMessage());
                throw new ClientException("Incorrect file structure provided;", ILLEGAL_ARGUMENTS_PROVIDED);
            }
        }

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
    }

    private boolean isBankFile(String txtFile) {
        return txtFile.startsWith(":25:");
    }

    public Long createAndFetchPaymentPackage(LocalDate date, Long collectionChannelId) {
        LocalDateTime startOfDay = date.atStartOfDay();

        CacheObject cacheObject = accountingPeriodsRepository
                .findAccountingPeriodsByDate(startOfDay)
                .orElseThrow(() -> new DomainEntityNotFoundException("Accounting period not found for date: " + startOfDay + ";"));

        PaymentPackageCreateRequest paymentPackageCreateRequest = new PaymentPackageCreateRequest();
        paymentPackageCreateRequest.setLockStatus(PaymentPackageLockStatus.UNLOCKED);
        paymentPackageCreateRequest.setAccountingPeriodId(cacheObject.getId());
        paymentPackageCreateRequest.setType(PaymentPackageType.OFFLINE);
        paymentPackageCreateRequest.setChannelId(collectionChannelId);
        paymentPackageCreateRequest.setPaymentDate(date);

        return paymentPackageService.create(paymentPackageCreateRequest);
    }

    private String detectEncoding(byte[] bytes) {
        UniversalDetector detector = new UniversalDetector(null);
        detector.handleData(bytes, 0, bytes.length);
        detector.dataEnd();
        String encoding = detector.getDetectedCharset();
        detector.reset();
        return encoding != null ? encoding : StandardCharsets.UTF_8.name();
    }
}
