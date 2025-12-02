package bg.energo.phoenix.service.receivable.paymentPackage;

import bg.energo.phoenix.exception.*;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.accountingPeriod.AccountingPeriods;
import bg.energo.phoenix.model.entity.receivable.collectionChannel.CollectionChannel;
import bg.energo.phoenix.model.entity.receivable.paymentPackage.PaymentPackage;
import bg.energo.phoenix.model.entity.receivable.paymentPackage.PaymentPackageFiles;
import bg.energo.phoenix.model.entity.receivable.paymentPackage.PaymentPackageStatusChangeHistory;
import bg.energo.phoenix.model.enums.billing.accountingsPeriods.AccountingPeriodStatus;
import bg.energo.phoenix.model.enums.receivable.paymentPackage.PaymentPackageListingType;
import bg.energo.phoenix.model.enums.receivable.paymentPackage.PaymentPackageLockStatus;
import bg.energo.phoenix.model.enums.receivable.paymentPackage.PaymentPackageSortingType;
import bg.energo.phoenix.model.enums.receivable.paymentPackage.PaymentPackageType;
import bg.energo.phoenix.model.request.receivable.paymentPackage.PaymentPackageCreateRequest;
import bg.energo.phoenix.model.request.receivable.paymentPackage.PaymentPackageEditRequest;
import bg.energo.phoenix.model.request.receivable.paymentPackage.PaymentPackageListingRequest;
import bg.energo.phoenix.model.response.receivable.collectionChannel.CollectionChannelShortResponse;
import bg.energo.phoenix.model.response.receivable.paymentPackage.*;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.repository.billing.accountingPeriods.AccountingPeriodsRepository;
import bg.energo.phoenix.repository.receivable.collectionChannel.CollectionChannelRepository;
import bg.energo.phoenix.repository.receivable.payment.PaymentRepository;
import bg.energo.phoenix.repository.receivable.paymentPackage.PaymentPackageFilesRepository;
import bg.energo.phoenix.repository.receivable.paymentPackage.PaymentPackageRepository;
import bg.energo.phoenix.repository.receivable.paymentPackage.PaymentPackageStatusChangeHistoryRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.util.ByteMultiPartFile;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import bg.energo.phoenix.util.epb.EPBJsonUtils;
import bg.energo.phoenix.util.epb.EPBListUtils;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.chrono.ChronoLocalDate;
import java.util.*;

import static bg.energo.phoenix.permissions.PermissionContextEnum.PAYMENT_PACKAGE;
import static bg.energo.phoenix.permissions.PermissionEnum.PAYMENT_PACKAGE_VIEW_DELETED;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentPackageService {

    private static final String SHEET = "Payments";
    private final static String FOLDER_PATH = "PaymentPackageFiles";
    private final PaymentPackageStatusChangeHistoryRepository paymentPackageHistoryRepository;
    private final CollectionChannelRepository collectionChannelRepository;
    private final AccountingPeriodsRepository accountingPeriodsRepository;
    private final PaymentPackageRepository paymentPackageRepository;
    private final PaymentPackageFilesRepository paymentPackageFilesRepository;
    private final PaymentRepository paymentRepository;
    private final PermissionService permissionService;
    private final FileService fileService;
    @Value("${ftp.server.base.path}")
    private String ftpBasePath;

    @Transactional
    public Long create(PaymentPackageCreateRequest request) {
        log.info("Create payment package: %s;".formatted(request));
        List<String> errorMessages = new ArrayList<>();

        PaymentPackage paymentPackage = new PaymentPackage();
        paymentPackage.setLockStatus(request.getLockStatus());
        checkAndSetLockStatus(paymentPackage, request.getLockStatus(), errorMessages);
        paymentPackage.setStatus(EntityStatus.ACTIVE);
        validateAndSetCollectionChannel(request.getChannelId(), paymentPackage, errorMessages);
        validateAndSetAccountingPeriodAndPaymentDate(request.getAccountingPeriodId(), request.getPaymentDate(), paymentPackage, errorMessages);
        paymentPackage.setType(request.getType());

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        paymentPackageRepository.save(paymentPackage);
        return paymentPackage.getId();
    }

    /**
     * Creates a new PaymentPackage from an online payment.
     *
     * This method validates and sets the accounting period, payment date,
     * collection channel, and other properties for the PaymentPackage.
     * It throws an exception if any validation errors occur.
     *
     * @param paymentPackageType the type of the payment package
     * @param lockStatus the lock status of the payment package
     * @param collectionChannelId the ID of the collection channel
     * @param accountingPeriodId the ID of the accounting period
     * @param paymentDate the date of the payment
     * @return the created PaymentPackage
     * @throws Exception if there are validation errors and rollbacks transaction
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public PaymentPackage createFromOnlinePayment(
            PaymentPackageType paymentPackageType,
            PaymentPackageLockStatus lockStatus,
            Long collectionChannelId,
            Long accountingPeriodId,
            LocalDate paymentDate
    ) {
        log.debug("Starting creation of payment package from online payment.");

        List<String> errorMessages = new ArrayList<>();
        PaymentPackage paymentPackage = new PaymentPackage();

        log.debug("Validating accounting period and payment date.");
        validateAndSetAccountingPeriodAndPaymentDate(accountingPeriodId, paymentDate, paymentPackage, errorMessages);

        log.debug("Validating collection channel.");
        validateAndSetCollectionChannel(collectionChannelId, paymentPackage, errorMessages);

        log.debug("Setting payment package properties.");
        paymentPackage.setStatus(EntityStatus.ACTIVE);
        paymentPackage.setType(paymentPackageType);
        paymentPackage.setLockStatus(lockStatus);

        log.debug("Checking for validation errors.");
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

        log.info("Saving payment package: %s".formatted(EPBJsonUtils.asJsonString(paymentPackage)));
        paymentPackage.setSystemUserId("system.admin");
        paymentPackage = paymentPackageRepository.saveAndFlush(paymentPackage);
        return paymentPackage;
    }

    private void checkAndSetLockStatus(PaymentPackage paymentPackage, PaymentPackageLockStatus requestedLockStatus, List<String> exceptionMessages) {
        if (!PaymentPackageLockStatus.UNLOCKED.equals(requestedLockStatus)) {
            exceptionMessages.add("lockStatus-[lockStatus] when payment package is created manually lock status must be unlocked;");
        }
        paymentPackage.setLockStatus(requestedLockStatus);
    }

    private void validateAndSetCollectionChannel(Long collectionChannelId, PaymentPackage paymentPackage, List<String> exceptionMessages) {
        Optional<CollectionChannel> collectionChannel = collectionChannelRepository.findCollectionChannelByIdAndStatusIn(collectionChannelId, List.of(EntityStatus.ACTIVE));
        if (collectionChannel.isEmpty()) {
            exceptionMessages.add("channelId-[channelId] active collection channel with: %s can't be found;".formatted(collectionChannelId));
        } else {
            paymentPackage.setCollectionChannelId(collectionChannelId);
        }

    }

    private CollectionChannel validateAndGetCollectionChannel(Long collectionChannelId) {
        return collectionChannelRepository
                .findById(collectionChannelId)
                .orElseThrow(
                        () -> new DomainEntityNotFoundException("Collection channel does not exists with given id [%s];".formatted(collectionChannelId))
                );
    }

    private void validateAndSetAccountingPeriodAndPaymentDate(Long accountingPeriodId, LocalDate paymentDate, PaymentPackage paymentPackage, List<String> exceptionMessages) {
        Optional<AccountingPeriods> accountingPeriodsOptional = accountingPeriodsRepository.findByIdAndStatus(accountingPeriodId, AccountingPeriodStatus.OPEN);
        if (accountingPeriodsOptional.isEmpty()) {
            exceptionMessages.add("accountingPeriodId-[accountingPeriodId] open accounting period with: %s can't be found;".formatted(accountingPeriodId));
        } else {
            AccountingPeriods accountingPeriod = accountingPeriodsOptional.get();
            if (paymentDate.isBefore(ChronoLocalDate.from(accountingPeriod.getStartDate())) || paymentDate.isAfter(ChronoLocalDate.from(accountingPeriod.getEndDate()))) {
                exceptionMessages.add("paymentDate-[paymentDate] should be in accounting period range: from %s to %s".formatted(accountingPeriod.getStartDate(), accountingPeriod.getEndDate()));
            }
            paymentPackage.setAccountingPeriodId(accountingPeriodId);
            paymentPackage.setPaymentDate(paymentDate);
        }
    }

    private AccountingPeriods validateAndGetAccountingPeriod(Long accountingPeriodId) {
        return accountingPeriodsRepository
                .findById(accountingPeriodId)
                .orElseThrow(
                        () -> new DomainEntityNotFoundException("Accounting period does not exists with given id [%s];".formatted(accountingPeriodId))
                );
    }

    @Transactional
    public PaymentPackageResponse view(Long id) {
        log.info("view for payment package with id: %s ".formatted(id));

        PaymentPackage paymentPackage = paymentPackageRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Can't find payment package with id: %s;".formatted(id)));

        checkPermissionsOnView(paymentPackage.getStatus(), paymentPackage.getId());

        PaymentPackageResponse paymentPackageResponse = new PaymentPackageResponse();
        paymentPackageResponse.setAccountingPeriod(new AccountingPeriodShortResponse(validateAndGetAccountingPeriod(paymentPackage.getAccountingPeriodId())));
        paymentPackageResponse.setCollectionChannel(new CollectionChannelShortResponse(validateAndGetCollectionChannel(paymentPackage.getCollectionChannelId())));

        PaymentPackageFiles paymentPackageFile = paymentPackageFilesRepository.findByPaymentPackageId(id);
        List<PaymentPackageErrorProtocolResponse> errorProtocolResponses = paymentPackageRepository.findReceivablesAndLiabilities(id);

        if (Objects.isNull(paymentPackageFile)) {
            handleMissingPaymentPackageFile(errorProtocolResponses, id, paymentPackageResponse);
        } else {
            try {
                handleExistingPaymentPackageFile(paymentPackageFile, errorProtocolResponses, id, paymentPackageResponse);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        Optional<List<PaymentPackageStatusChangeHistory>> statusChangeHistoryOptional = paymentPackageHistoryRepository.findAllByPaymentPackageId(paymentPackage.getId());
        if (statusChangeHistoryOptional.isPresent()) {
            List<PaymentPackageStatusChangeHistoryShortResponse> statusChangeHistory = statusChangeHistoryOptional
                    .get()
                    .stream()
                    .map(PaymentPackageStatusChangeHistoryShortResponse::new)
                    .toList();

            paymentPackageResponse.setStatusChangeHistory(statusChangeHistory);
        }

        paymentPackageResponse.setId(paymentPackage.getId());
        paymentPackageResponse.setLockStatus(paymentPackage.getLockStatus());
        paymentPackageResponse.setPaymentDate(paymentPackage.getPaymentDate());
        paymentPackageResponse.setEntityStatus(paymentPackage.getStatus());

        return paymentPackageResponse;
    }

    @Transactional
    public Long edit(Long id, PaymentPackageEditRequest request) {
        log.debug("Editing payment package with id: {}", id);

        PaymentPackage paymentPackage = paymentPackageRepository
                .findById(id)
                .orElseThrow(() -> new ClientException("Can't find payment package with id: %s;".formatted(id), ErrorCode.DOMAIN_ENTITY_NOT_FOUND));

        if (paymentPackage.getStatus().equals(EntityStatus.DELETED)) {
            log.error("Unable to edit deleted payment package;");
            throw new ClientException("Unable to edit deleted payment package;", ErrorCode.APPLICATION_ERROR);
        }

        paymentPackage.setLockStatus(request.getLockStatus());
        paymentPackageRepository.save(paymentPackage);
        return paymentPackage.getId();
    }

    public Long delete(Long id) {
        log.debug("Deleting payment package with id: {}", id);

        PaymentPackage paymentPackage = paymentPackageRepository
                .findById(id)
                .orElseThrow(() -> new ClientException("Can't find payment package with id: %s;".formatted(id), ErrorCode.DOMAIN_ENTITY_NOT_FOUND));

        if (paymentPackage.getStatus().equals(EntityStatus.DELETED)) {
            log.error("Payment package is already deleted;");
            throw new ClientException("Payment package is already deleted;", ErrorCode.APPLICATION_ERROR);
        }

        if (paymentRepository.existsByPaymentPackageIdAndStatus(id, EntityStatus.ACTIVE)) {
            log.error("Payment package is connected to an active payment and can not be deleted;");
            throw new OperationNotAllowedException("Payment package is connected to an active payment and can not be deleted;");
        }

        paymentPackage.setStatus(EntityStatus.DELETED);
        paymentPackageRepository.save(paymentPackage);
        return paymentPackage.getId();
    }


    public Page<PaymentPackageListingResponse> list(PaymentPackageListingRequest request) {
        List<EntityStatus> paymentPackageStatuses = new ArrayList<>();
        if (hasDeletedPermission()) {
            paymentPackageStatuses.add(EntityStatus.DELETED);
        }
        if (hasViewPermission()) {
            paymentPackageStatuses.add(EntityStatus.ACTIVE);
        }

        return paymentPackageRepository.filter(
                ListUtils.emptyIfNull(request.getCollectionChannelIds()),
                EPBListUtils.convertEnumListIntoStringListIfNotNull(request.getLockStatuses()),
                request.getFromDate(),
                request.getToDate(),
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                Objects.requireNonNullElse(request.getSearchBy(), PaymentPackageListingType.ALL).name(),
                EPBListUtils.convertEnumListIntoStringListIfNotNull(paymentPackageStatuses),
                PageRequest.of(
                        request.getPage(),
                        request.getSize(),
                        Sort.by(
                                new Sort.Order(request.getDirection(), getSortingField(request.getSortingField()))
                        )
                )
        ).map(PaymentPackageListingResponse::new);
    }

    private String getSortingField(PaymentPackageSortingType sortingType) {
        return sortingType != null ? sortingType.getValue() : PaymentPackageSortingType.NUMBER.getValue();
    }

    private void checkPermissionsOnView(EntityStatus entityStatus, Long id) {
        switch (entityStatus) {
            case ACTIVE -> {
                if (!hasViewPermission()) {
                    log.error("You don’t have permission to view active payment package with ID: [%s]".formatted(id));
                    throw new AccessDeniedException("You don’t have permission to view active payment package with ID: [%s]".formatted(id));
                }
            }
            case DELETED -> {
                if (!hasDeletedPermission()) {
                    log.error("You don’t have permission to view deleted payment package with ID: [%s]".formatted(id));
                    throw new AccessDeniedException("You don’t have permission to view deleted payment package with ID: [%s]".formatted(id));
                }
            }
        }
    }

    private boolean hasDeletedPermission() {
        return permissionService.permissionContextContainsPermissions(PAYMENT_PACKAGE, List.of(PAYMENT_PACKAGE_VIEW_DELETED));
    }

    private boolean hasViewPermission() {
        return permissionService.permissionContextContainsPermissions(PAYMENT_PACKAGE, List.of(PermissionEnum.PAYMENT_PACKAGE_VIEW));
    }

    /**
     * Generates a report for a payment package and uploads the report to a remote file service.
     *
     * @param errorProtocolResponse a list of payment package error protocol responses
     * @param paymentPackageId      the ID of the payment package
     * @return the ID of the generated payment package file
     * @throws IOException if an I/O error occurs during the file upload
     */
    private PaymentPackageErrorProtocolFileResponse generateErrorProtocolFile(List<PaymentPackageErrorProtocolResponse> errorProtocolResponse, Long paymentPackageId, Boolean updateExistingFile) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        createWorkBook(errorProtocolResponse, byteArrayOutputStream);

        String fileName = getFileName();
        MultipartFile multipartFile = new ByteMultiPartFile(fileName, byteArrayOutputStream.toByteArray());
        String fileUrl = fileService.uploadFile(multipartFile, remotePath(), fileName);

        byteArrayOutputStream.close();
        PaymentPackageFiles paymentPackageFiles;
        if (updateExistingFile) {
            paymentPackageFiles = paymentPackageFilesRepository.findByPaymentPackageId(paymentPackageId);
        } else {
            paymentPackageFiles = new PaymentPackageFiles();
            paymentPackageFiles.setPaymentPackageId(paymentPackageId);
        }
        paymentPackageFiles.setName(fileName);
        paymentPackageFiles.setFileUrl(fileUrl);
        paymentPackageFilesRepository.saveAndFlush(paymentPackageFiles);
        return new PaymentPackageErrorProtocolFileResponse(paymentPackageFiles);
    }

    /**
     * Creates an Excel workbook with a sheet containing payment package error protocol responses.
     *
     * @param errorProtocolResponse the list of payment package error protocol responses to include in the workbook
     * @param outputStream          the output stream to write the workbook to
     * @throws IOException if an I/O error occurs while writing the workbook
     */
    private void createWorkBook(List<PaymentPackageErrorProtocolResponse> errorProtocolResponse, OutputStream outputStream) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(SHEET);
            Row headerRow = sheet.createRow(0);
            Cell headerCell1 = headerRow.createCell(0);
            headerCell1.setCellValue("Payment_ID");
            Cell headerCell2 = headerRow.createCell(1);
            headerCell2.setCellValue("Initial_amount");
            Cell headerCell3 = headerRow.createCell(2);
            headerCell3.setCellValue("Amount_not_used_in_liability_cover");
            Cell headerCell4 = headerRow.createCell(3);
            headerCell4.setCellValue("Linked receivable");

            int rowNum = 1;
            for (PaymentPackageErrorProtocolResponse payment : errorProtocolResponse) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(payment.paymentId());
                row.createCell(1).setCellValue(payment.initialAmount());
                row.createCell(2).setCellValue(payment.currentAmount());
                row.createCell(3).setCellValue(payment.linkedReceivableId());
            }
            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);
            sheet.autoSizeColumn(2);
            sheet.autoSizeColumn(3);
            workbook.write(outputStream);
        }
    }

    /**
     * Generates a unique file name by combining a UUID with the original file name, with any whitespace removed.
     *
     * @return the generated file name
     */
    private String getFileName() {
        return String.format("%s_%s.xlsx", UUID.randomUUID(), "ErrorProtocols");
    }

    /**
     * Retrieves the remote path for the payment package, which is constructed by combining the FTP base path, the folder path, and the current date.
     *
     * @return the remote path for the payment package
     */
    private String remotePath() {
        return String.format("%s/%s/%s", ftpBasePath, FOLDER_PATH, LocalDate.now());
    }

    /**
     * Downloads a payment package file by its ID.
     *
     * @param id the ID of the payment package file to download
     * @return a {@link PaymentPackageErrorProtocolFileContent} containing the file name and byte array of the downloaded file
     * @throws DomainEntityNotFoundException if the file with the given ID is not found
     */
    public PaymentPackageErrorProtocolFileContent download(Long id) {
        PaymentPackageFiles paymentPackageFiles = paymentPackageFilesRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("File with presented id not found;"));

        ByteArrayResource resource = fileService.downloadFile(paymentPackageFiles.getFileUrl());

        return new PaymentPackageErrorProtocolFileContent(paymentPackageFiles.getName(), resource.getByteArray());
    }

    /**
     * Handles the case where the payment package file is not in DB yet.
     * If the list of error protocol responses is not empty, it generates a new error protocol file and sets its ID in the response.
     *
     * @param errorProtocolResponses List of PaymentPackageErrorProtocolResponse containing the receivables and liabilities.
     * @param id                     The ID of the payment package.
     * @param paymentPackageResponse The response object where the generated error protocol file ID will be set.
     */
    private void handleMissingPaymentPackageFile(List<PaymentPackageErrorProtocolResponse> errorProtocolResponses, Long id, PaymentPackageResponse paymentPackageResponse) {
        if (CollectionUtils.isNotEmpty(errorProtocolResponses)) {
            try {
                paymentPackageResponse.setErrorProtocolFileId(generateErrorProtocolFile(errorProtocolResponses, id, false));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Handles the case where the payment package file exists.
     * It checks the number of filled rows in the existing error protocol file and compares it to the size of the error protocol responses.
     * If the number of rows is less than the number of responses, a new error protocol file is generated; otherwise, the existing file is used.
     *
     * @param paymentPackageFile     The existing payment package file to be processed.
     * @param errorProtocolResponses List of PaymentPackageErrorProtocolResponse containing the receivables and liabilities.
     * @param id                     The ID of the payment package.
     * @param paymentPackageResponse The response object where the generated or existing error protocol file ID will be set.
     * @throws IOException If an I/O error occurs while processing the file.
     */
    private void handleExistingPaymentPackageFile(PaymentPackageFiles paymentPackageFile, List<PaymentPackageErrorProtocolResponse> errorProtocolResponses, Long id, PaymentPackageResponse paymentPackageResponse) throws IOException {
        int filledRowCountWithoutHeader = getFilledRowCountWithoutHeader(paymentPackageFile);
        if (filledRowCountWithoutHeader < errorProtocolResponses.size()) {
            paymentPackageResponse.setErrorProtocolFileId(generateErrorProtocolFile(errorProtocolResponses, id, true));
        } else {
            paymentPackageResponse.setErrorProtocolFileId(new PaymentPackageErrorProtocolFileResponse(paymentPackageFile));
        }
    }

    /**
     * Retrieves the number of filled rows (excluding the header) from the given payment package file.
     * It downloads the file content, opens it as an Excel workbook, and calculates the number of rows in the first sheet.
     *
     * @param paymentPackageFile The payment package file to be analyzed.
     * @return The number of filled rows in the file, excluding the header row.
     * @throws RuntimeException If an I/O error occurs while processing the file content.
     */
    private int getFilledRowCountWithoutHeader(PaymentPackageFiles paymentPackageFile) {
        PaymentPackageErrorProtocolFileContent fileContent = download(paymentPackageFile.getId());
        byte[] content = fileContent.content();
        try (ByteArrayInputStream baIs = new ByteArrayInputStream(content);
             XSSFWorkbook workbook = new XSSFWorkbook(baIs)) {
            XSSFSheet sheet = workbook.getSheetAt(0);
            return sheet.getPhysicalNumberOfRows() - 1;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Blocks all payment packages created during the current day by setting their status to LOCKED.
     * Gets packages between start of day (00:00:00) and end of day (23:59:59), updates their lock status.
     */
    public void onlinePackageBlocker() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = now.toLocalDate().atTime(LocalTime.MAX);

        List<PaymentPackage> todayCreatedPackages = paymentPackageRepository.getTodayCreatedPackage(startOfDay, endOfDay);

        for (PaymentPackage paymentPackage : todayCreatedPackages) {
            paymentPackage.setLockStatus(PaymentPackageLockStatus.LOCKED);
            paymentPackageRepository.save(paymentPackage);
        }
    }


}
