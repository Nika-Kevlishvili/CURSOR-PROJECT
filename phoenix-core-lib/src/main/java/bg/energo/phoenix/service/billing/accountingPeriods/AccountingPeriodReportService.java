package bg.energo.phoenix.service.billing.accountingPeriods;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.accountingPeriod.files.AccountPeriodSapReport;
import bg.energo.phoenix.model.entity.billing.accountingPeriod.files.AccountPeriodVatDairyReport;
import bg.energo.phoenix.model.enums.billing.accountingsPeriods.AccountPeriodFileGenerationStatus;
import bg.energo.phoenix.model.enums.billing.accountingsPeriods.AccountingPeriodReportType;
import bg.energo.phoenix.model.response.billing.accountingPeriods.AccountPeriodSapResponse;
import bg.energo.phoenix.model.response.billing.accountingPeriods.AccountPeriodVatResponse;
import bg.energo.phoenix.model.response.proxy.FileContent;
import bg.energo.phoenix.repository.billing.accountingPeriods.AccountingPeriodsRepository;
import bg.energo.phoenix.repository.billing.accountingPeriods.files.AccountingPeriodSapReportRepository;
import bg.energo.phoenix.repository.billing.accountingPeriods.files.AccountingPeriodVatDairyReportRepository;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.util.ByteMultiPartFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountingPeriodReportService {

    private final AccountingPeriodsRepository accountingPeriodsRepository;
    private final FileService fileService;
    private final AccountingPeriodSapReportRepository accountingPeriodSapReportRepository;
    private final AccountingPeriodVatDairyReportRepository accountingPeriodVatDairyReportRepository;
    @Value("${ftp.server.base.path}")
    private String ftpBasePath;
    @Value("${billing.account-period-sap.number-of-threads}")
    private Integer sapNumberOfThreads;
    @Value("${billing.account-period-sap.query-chunk-size}")
    private Integer sapChunkSize;
    private final TransactionTemplate transactionTemplate;
    private static final String SAP_FOLDER_PATH = "account_period_sap_reports";
    @Value("${billing.account-period-vat-dairy.number-of-threads}")
    private Integer vatNumberOfThreads;
    @Value("${billing.account-period-vat-dairy.query-chunk-size}")
    private Integer vatChunkSize;
    private static final String VAT_FOLDER_PATH = "account_period_vat_dairy_reports";

    /**
     * Generates an Excel report for the accounting period with the specified ID.
     *
     * @param id                   The ID of the accounting period.
     * @param fileGenerationStatus The status of the file generation.
     */
    public void generateExcel(Long id, AccountPeriodFileGenerationStatus fileGenerationStatus) {

        log.debug("start generating excel for account period with id {}", id);

        boolean regenerate = Objects.equals(fileGenerationStatus, AccountPeriodFileGenerationStatus.FAILED) || accountingPeriodsRepository.mustRegenerateSAP(id);

        Long size = accountingPeriodsRepository.countAllIncomeAccs(id, regenerate);
        log.debug("total size of income account numbers retrieved: {}", size);

        AccountPeriodSapReport entity;

        if (size > 0) {
            int threadCount = Math.min((int) ((size / sapChunkSize) + 1), sapNumberOfThreads);
            log.debug("number of threads used: {}", threadCount);

            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

            List<AccountPeriodSapResponse> allData = new ArrayList<>();
            AtomicInteger offset = new AtomicInteger(0);
            int retrievedSize = 0;
            byte[] bArray;
            List<Callable<List<AccountPeriodSapResponse>>> tasks = new ArrayList<>();

            while (retrievedSize < size) {
                tasks.add(() -> {
                    List<AccountPeriodSapResponse> data = accountingPeriodsRepository.getDataForSap(id, regenerate, offset.getAndAdd(sapChunkSize), sapChunkSize);
                    allData.addAll(data);
                    return data;
                });
                retrievedSize += sapChunkSize;
            }
            try {
                executorService.invokeAll(tasks);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            executorService.shutdown();

            log.debug("total size of data grouped by income account number retrieved: {}", allData.size());
            if (CollectionUtils.isNotEmpty(allData)) {
                ByteArrayOutputStream bos;
                try (Workbook workbook = new XSSFWorkbook()) {
                    Sheet sheet = workbook.createSheet("САП_%s г.".formatted(allData.get(0).getLastDay().format(DateTimeFormatter.ofPattern("MM.yyyy"))));
                    CellStyle style = workbook.createCellStyle();
                    DataFormat format = workbook.createDataFormat();
                    style.setDataFormat(format.getFormat("0.00"));

                    CellStyle totVolStyle = workbook.createCellStyle();
                    DataFormat totVolFormat = workbook.createDataFormat();
                    totVolStyle.setDataFormat(totVolFormat.getFormat("0.0000"));

                    int rowNum = 0;
                    for (AccountPeriodSapResponse response : allData) {
                        Row row = sheet.createRow(rowNum++);
                        row.createCell(0).setCellValue(60000);
                        row.createCell(1).setCellValue(response.getLastDay().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                        row.createCell(2).setCellValue(response.getIncomeAccountNumber());

                        Cell totAmtCell = row.createCell(3);
                        totAmtCell.setCellValue(response.getTotalAmount().setScale(2, RoundingMode.HALF_UP).doubleValue());
                        totAmtCell.setCellStyle(style);

                        Cell totAmtOfVatCell = row.createCell(4);
                        totAmtOfVatCell.setCellValue(response.getTotalAmountOfVat().setScale(2, RoundingMode.HALF_UP).doubleValue());
                        totAmtOfVatCell.setCellStyle(style);

                        Cell totVolCell = row.createCell(5);
                        totVolCell.setCellValue(response.getTotalVolumes().setScale(2, RoundingMode.HALF_UP).doubleValue());
                        totVolCell.setCellStyle(totVolStyle);

                        row.createCell(6).setCellValue(Objects.nonNull(response.getTotalVolumes()) ? "MWH" : null);
                        row.createCell(7).setCellValue(response.getIncomeAccountName());
                    }
                    for (int i = 0; i <= 7; i++) {
                        sheet.autoSizeColumn(i);
                    }

                    bos = new ByteArrayOutputStream();
                    workbook.write(bos);

                } catch (Exception e) {
                    log.error("error during generation of excel report, {}", e.getMessage());
                    throw new RuntimeException("error happened while generating excel report for account period");
                }
                bArray = bos.toByteArray();

                String randomName = UUID.randomUUID().toString();
                MultipartFile multipartFile = new ByteMultiPartFile(randomName, bArray);
                String name = "SAP Excel Report (".concat(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM.dd HH:mm")))
                        .concat(")").concat(".xlsx");
                String fileUrl = fileService.uploadFile(multipartFile, String.format("%s/%s/%s", ftpBasePath, SAP_FOLDER_PATH, id),
                        String.format("%s_%s", randomName, name));
                entity = new AccountPeriodSapReport(null, name, fileUrl, id, EntityStatus.ACTIVE);
            } else {
                entity = null;
            }
        } else {
            entity = null;
        }


        transactionTemplate.executeWithoutResult(a -> {
            if (regenerate) {
                accountingPeriodSapReportRepository.deleteAllByAccountPeriodId(id);
            }
            if (Objects.nonNull(entity)) {
                accountingPeriodSapReportRepository.save(entity);
            }
        });
    }

    /**
     * Generates a VAT Dairy report for the specified accounting period.
     * <p>
     * This method retrieves all invoices for the given accounting period, processes them in parallel, and generates a VAT Dairy report file. The report is then uploaded to the configured file storage location.
     *
     * @param id     The ID of the accounting period for which to generate the VAT Dairy report.
     * @param status The status of the file generation process, used to determine if the report needs to be regenerated.
     */
    public void generateVatDairy(Long id, AccountPeriodFileGenerationStatus status) {
        log.debug("start generating vat dairy for account period with id {}", id);

        boolean regenerate = Objects.equals(status, AccountPeriodFileGenerationStatus.FAILED) || accountingPeriodsRepository.mustRegenerateVatDairy(id);

        AccountPeriodVatDairyReport entity;

        Long size = accountingPeriodsRepository.countAllInvoices(id, regenerate);
        log.debug("total size of invoices retrieved: {}", size);
        if (size > 0) {
            int threadCount = Math.min((int) ((size / vatChunkSize) + 1), vatNumberOfThreads);
            log.debug("number of threads used: {}", threadCount);

            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

            List<AccountPeriodVatResponse> allData = new ArrayList<>();
            AtomicInteger offset = new AtomicInteger(0);
            int retrievedSize = 0;
            byte[] bArray;
            List<Callable<List<AccountPeriodVatResponse>>> tasks = new ArrayList<>();

            while (retrievedSize < size) {
                tasks.add(() -> {
                    List<AccountPeriodVatResponse> data = accountingPeriodsRepository.getDataForVatDairy(id, regenerate, offset.getAndAdd(vatChunkSize), vatChunkSize);
                    allData.addAll(data);
                    return data;
                });
                retrievedSize += vatChunkSize;
            }
            try {
                executorService.invokeAll(tasks);
            } catch (InterruptedException e) {
                throw new RuntimeException("Exception during executing callables");
            }
            executorService.shutdown();

            log.debug("total size of invoice data retrieved: {}", allData.size());
            if (CollectionUtils.isNotEmpty(allData)) {
                List<AccountPeriodVatResponse> finalList = allData.stream().sorted(Comparator.comparing(AccountPeriodVatResponse::getRowNum))
                        .toList();

                try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                    for (AccountPeriodVatResponse row : finalList) {
                        bos.write(row.getValue().getBytes(StandardCharsets.UTF_8));
                        bos.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));
                    }
                    bArray = bos.toByteArray();

                } catch (Exception e) {
                    log.error("error during generation of vat dairy report, {}", e.getMessage());
                    throw new RuntimeException("error happened while generating vat dairy report for account period");
                }

                String randomName = UUID.randomUUID().toString();
                MultipartFile multipartFile = new ByteMultiPartFile(randomName, bArray);
                String name = "VAT Dairy reports (".concat(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM.dd HH:mm")))
                        .concat(")").concat(".txt");
                String fileUrl = fileService.uploadFile(multipartFile, String.format("%s/%s/%s", ftpBasePath, VAT_FOLDER_PATH, id),
                        String.format("%s_%s", randomName, name));
                entity = new AccountPeriodVatDairyReport(null, name, fileUrl, id, EntityStatus.ACTIVE);
            } else {
                entity = null;
            }
        } else {
            entity = null;
        }


        transactionTemplate.executeWithoutResult(a -> {
            if (regenerate) {
                accountingPeriodVatDairyReportRepository.deleteAllByAccountPeriodId(id);
            }
            if (Objects.nonNull(entity)) {
                accountingPeriodVatDairyReportRepository.save(entity);
            }
        });
    }

    /**
     * Downloads a file based on the provided accounting period report type.
     *
     * @param id         The ID of the accounting period report.
     * @param reportType The type of accounting period report to download.
     * @return A {@link FileContent} object containing the file name and byte array of the downloaded file.
     * @throws DomainEntityNotFoundException If the requested file is not found.
     */
    public FileContent downloadFile(Long id, AccountingPeriodReportType reportType) {
        AccountPeriodSapReport sapReport;
        AccountPeriodVatDairyReport vatDairyReport;
        if (reportType.equals(AccountingPeriodReportType.SAP_EXCEL)) {
            sapReport = accountingPeriodSapReportRepository.findById(id)
                    .orElseThrow(() -> new DomainEntityNotFoundException("id-Sap file with ID %s not found;".formatted(id)));
            return new FileContent(sapReport.getName(), fileService.downloadFile(sapReport.getFileUrl()).getByteArray());
        } else {
            vatDairyReport = accountingPeriodVatDairyReportRepository.findById(id)
                    .orElseThrow(() -> new DomainEntityNotFoundException("id-Vat Dairy file with ID %s not found;".formatted(id)));
            return new FileContent(vatDairyReport.getName(), fileService.downloadFile(vatDairyReport.getFileUrl()).getByteArray());
        }
    }
}
