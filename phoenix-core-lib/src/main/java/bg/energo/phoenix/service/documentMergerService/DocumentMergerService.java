package bg.energo.phoenix.service.documentMergerService;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.IllegalArgumentsProvidedException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.billingRun.BillingRun;
import bg.energo.phoenix.model.entity.billing.billingRun.BillingSumFile;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.entity.receivable.powerSupplyDisconnectionReminder.PowerSupplyDcnReminderDocFiles;
import bg.energo.phoenix.model.entity.receivable.powerSupplyDisconnectionReminder.PowerSupplyDisconnectionReminder;
import bg.energo.phoenix.model.entity.receivable.reminder.Reminder;
import bg.energo.phoenix.model.entity.receivable.reminder.ReminderDocumentFile;
import bg.energo.phoenix.model.entity.template.ContractTemplate;
import bg.energo.phoenix.model.entity.template.ContractTemplateDetail;
import bg.energo.phoenix.model.enums.documents.DocumentStatus;
import bg.energo.phoenix.process.controller.FtpTestController;
import bg.energo.phoenix.process.model.entity.ProcessFile;
import bg.energo.phoenix.process.repository.ProcessFileRepository;
import bg.energo.phoenix.repository.billing.billingRun.BillingSumFileRepository;
import bg.energo.phoenix.repository.documents.DocumentsRepository;
import bg.energo.phoenix.repository.receivable.powerSupplyDisconnectionReminder.PowerSupplyDcnReminderDocFileRepository;
import bg.energo.phoenix.repository.receivable.powerSupplyDisconnectionReminder.PowerSupplyDisconnectionReminderRepository;
import bg.energo.phoenix.repository.receivable.reminder.ReminderDocumentFileRepository;
import bg.energo.phoenix.repository.receivable.reminder.ReminderRepository;
import bg.energo.phoenix.repository.template.ContractTemplateDetailsRepository;
import bg.energo.phoenix.repository.template.ContractTemplateRepository;
import bg.energo.phoenix.service.document.ReminderDocumentGenerationService;
import bg.energo.phoenix.service.document.ReminderForDcnDocumentCreationService;
import bg.energo.phoenix.service.document.enums.FileFormat;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.util.ByteMultiPartFile;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;


@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentMergerService {
    private final FileService fileService;

    private final DocumentsRepository documentsRepository;
    private final ProcessFileRepository processFileRepository;
    private final BillingSumFileRepository billingSumFileRepository;
    private final ReminderDocumentFileRepository reminderDocumentFileRepository;
    private final ReminderDocumentGenerationService reminderDocumentGenerationService;
    private final ReminderRepository reminderRepository;
    private final PowerSupplyDcnReminderDocFileRepository powerSupplyDcnReminderDocFileRepository;
    private final ReminderForDcnDocumentCreationService reminderForDcnDocumentCreationService;
    private final PowerSupplyDisconnectionReminderRepository powerSupplyDisconnectionReminderRepository;
    private final ContractTemplateDetailsRepository contractTemplateDetailsRepository;
    private final ContractTemplateRepository contractTemplateRepository;
    private final FtpTestController ftpTestController;

    @Value("${document.merging.partition_size}")
    private Integer partitionSize;
    @Value("${document.merging.base_path}")
    private String mergeBasePath;

    @Value("${invoice.merging.bulkSize}")
    private Integer bulkSize;
    @Value("${invoice.merging.threadPoolSize}")
    private Integer threadPoolSize;
    @Value("${invoice.document.ftp_directory_path}")
    private String invoiceFtpPath;

    @Value("${reminder.document.ftp_directory_path}")
    private String reminderFtpPath;

    @Value("${disconnection_reminder.document.ftp_directory_path}")
    private String disconnectionReminderFtpPath;

    /**
     * Collects the results of a list of CompletableFuture tasks representing file downloads,
     * and returns a list of downloaded file names. Logs information about the results.
     *
     * @param bulkFutures a list of CompletableFuture objects, each representing
     *                    an asynchronous file downloading task.
     * @return a list of file names representing successfully downloaded or merged files,
     * or null if no files were downloaded or merged.
     */
    private static List<String> collectDownloadedFiles(List<CompletableFuture<String>> bulkFutures) {
        List<String> allDownloadedFiles = bulkFutures.stream()
                .map(CompletableFuture::join)
                .peek(file -> log.debug("Downloaded file: {}", file))
                .toList();

        if (allDownloadedFiles.isEmpty()) {
            log.debug("No files were downloaded or merged. Process aborted.");
            return null;
        }
        log.debug("Number of downloaded/merged files: {}", allDownloadedFiles.size());
        return allDownloadedFiles;
    }

    /**
     * Merges billing run documents by processing files associated with a specified billing run,
     * while excluding specific invoices from the merge process.
     *
     * @param billingId          The billing run object containing details of the billing cycle to process.
     * @param excludedInvoiceIds A list of invoice IDs to be excluded from the merging operation.
     */
    @BillingRunMergeOperation
    public void mergeBillingRunDocuments(BillingRun billingId, List<Long> excludedInvoiceIds) {
        MergeContext context = MergeContext.builder()
                .billingRun(billingId)
                .excludedInvoiceIds(excludedInvoiceIds)
                .baseDirectory(getOrCreateBaseDirectory(mergeBasePath))
                .ftpPath(invoiceFtpPath)
                .partitionSize(partitionSize)
                .build();
        mergeDocuments(
                context,
                this::fetchBillingRunFiles,
                this::saveBillingRunFile
        );
    }

    /**
     * Fetches the list of billing run files associated with the given billing run ID from the context.
     * Processes the documents in batches, downloads them asynchronously, and stores them in the specified base directory.
     *
     * @param context the merge context containing details such as the billing run ID, excluded invoice IDs,
     *                and the base directory for saving the downloaded files
     * @return a list of file paths of the downloaded billing run files, or null if no files were downloaded
     */
    @BillingRunMergeOperation
    private List<String> fetchBillingRunFiles(MergeContext context) {
        List<Long> excludedIds = new ArrayList<>();

        ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);
        List<CompletableFuture<String>> bulkFutures = new ArrayList<>();
        int i = 0;
        while (true) {
            log.debug("FetchingFilesForMerging page {}", i);
            Page<Document> documents = documentsRepository.findAllByBillingId(
                    context.getBillingRun().getId(),
                    context.excludedInvoiceIds,
                    PageRequest.of(i++, bulkSize)
            );
            log.debug("FileSizeForPage {} is {}", i - 1, documents.getNumberOfElements());

            if (documents.isEmpty()) {
                break;
            }

            excludedIds.addAll(documents.stream().map(Document::getId).toList());

            for (Document document : documents) {
                bulkFutures.add(
                        CompletableFuture.supplyAsync(() -> downloadAndSaveBillingFile(document, context.getBaseDirectory()))
                );
            }
        }

        List<String> allDownloadedFiles = new ArrayList<>(bulkFutures.stream().map(CompletableFuture::join).toList());
        executorService.shutdown();

        return allDownloadedFiles.isEmpty() ? null : allDownloadedFiles;
    }

    /**
     * Downloads a billing file from the provided document's signed file URL and saves it in the specified base directory
     * with a filename based on the document's ID.
     *
     * @param document      The document containing the signed file URL and ID used for downloading and naming the billing file.
     * @param baseDirectory The directory in which the downloaded billing file will be saved.
     * @return The path to the saved billing file as a string.
     */
    @BillingRunMergeOperation
    private String downloadAndSaveBillingFile(Document document, File baseDirectory) {
        return downloadAndSaveFile(
                document.getSignedFileUrl(),
                baseDirectory.getAbsolutePath(),
                document.getId() + ".pdf",
                "Error downloading file for document ID: {" + document.getId() + "}"
        );
    }

    /**
     * Saves the billing run file along with its related metadata into the database.
     *
     * @param context the {@code MergeContext} object containing the necessary data
     *                required to save the billing run file, including billing
     *                information, file path, and index details.
     */
    @BillingRunMergeOperation
    private void saveBillingRunFile(MergeContext context) {
        Document document = Document.builder()
                .signers(new ArrayList<>())
                .signedBy(new ArrayList<>())
                .name(context.getIndex() > 1
                        ? "%s_%s.pdf".formatted(context.getBillingRun().getBillingNumber(), context.getIndex())
                        : "%s.pdf".formatted(context.getBillingRun().getBillingNumber()))
                .unsignedFileUrl(context.getFilePath())
                .signedFileUrl(context.getFilePath())
                .fileFormat(FileFormat.PDF)
                .templateId(null)
                .documentStatus(DocumentStatus.SIGNED)
                .status(EntityStatus.ACTIVE)
                .build();
        documentsRepository.saveAndFlush(document);
        BillingSumFile sumFile = BillingSumFile.builder()
                .documentId(document.getId())
                .billingId(context.getBillingRun().getId())
                .build();
        billingSumFileRepository.save(sumFile);
    }

    /**
     * Merges reminder documents associated with the specified reminder ID and process ID.
     *
     * @param reminderId the ID of the reminder whose documents need to be merged
     * @param processId  the ID of the process under which the reminder operates
     */
    @ReminderMergeOperation
    public void mergeReminderDocuments(Long reminderId, Long processId) {
        MergeContext context = MergeContext.builder()
                .reminderId(reminderId)
                .processId(processId)
                .baseDirectory(getOrCreateBaseDirectory(mergeBasePath))
                .ftpPath(reminderFtpPath)
                .partitionSize(partitionSize)
                .build();
        mergeDocuments(
                context,
                this::fetchAndSaveReminderFiles,
                this::saveReminderFile
        );
    }

    /**
     * Fetches reminder files from the repository and saves them locally by downloading and processing each file asynchronously.
     *
     * @param context the context containing information about the reminder and its associated base directory
     * @return a list of file paths for the saved reminder files
     */
    @ReminderMergeOperation
    private List<String> fetchAndSaveReminderFiles(MergeContext context) {
        List<CompletableFuture<String>> bulkFutures = new ArrayList<>();
        Optional<List<ReminderDocumentFile>> reminderDocumentFileOptional = reminderDocumentFileRepository.findByReminderId(context.reminderId);
        if (reminderDocumentFileOptional.isPresent()) {
            List<ReminderDocumentFile> reminderDocumentFile = reminderDocumentFileOptional.get();
            if (!reminderDocumentFile.isEmpty()) {
                for (ReminderDocumentFile document : reminderDocumentFile) {
                    bulkFutures.add(CompletableFuture.supplyAsync(() -> downloadAndSaveReminderFile(document, context.baseDirectory)));
                }
            }
        }
        return collectDownloadedFiles(bulkFutures);
    }

    /**
     * Downloads a reminder document file from the specified URL, saves it to the given
     * base directory, and returns the path to the saved file. Applies a naming convention
     * for the saved file and handles errors during the download process.
     *
     * @param document      the ReminderDocumentFile object containing the file URL, reminder ID,
     *                      and document ID used to generate the saved file name.
     * @param baseDirectory the base directory where the downloaded file will be saved.
     * @return the absolute path of the saved file as a String.
     */
    @ReminderMergeOperation
    private String downloadAndSaveReminderFile(ReminderDocumentFile document, File baseDirectory) {
        log.info("Starting to download reminder file for document ID: {}", document.getId());
        return downloadAndSaveFile(
                document.getFileUrl(),
                baseDirectory.getAbsolutePath(),
                "Reminder-%s-".formatted(document.getReminderId()) + document.getId() + ".pdf",
                "Error downloading file for document ID: {" + document.getId() + "}"
        );
    }

    /**
     * Saves a reminder file based on the provided merge context.
     *
     * @param context the context containing details such as reminder ID, file path,
     *                process ID, and index, which are used to construct and save the reminder file.
     */
    @ReminderMergeOperation
    private void saveReminderFile(MergeContext context) {
        Reminder reminder = reminderRepository.findById(context.reminderId)
                .orElseThrow(() -> new IllegalArgumentException("Reminder not found for ID: " + context.reminderId));
        String fileName = reminderDocumentGenerationService.formatDocumentFileName(reminder, reminder.getDocumentTemplateId(), (long) context.index);

        ProcessFile processFile = ProcessFile.builder()
                .name(context.index > 1 ? "%s_%s.pdf".formatted(fileName, context.index) : "%s.pdf".formatted(fileName))
                .fileUrl(context.filePath)
                .processId(context.processId)
                .build();
        processFileRepository.saveAndFlush(processFile);
    }

    /**
     * Merges the generated Power Supply Disconnection Reminder (PSDR) documents with the corresponding disconnection reminder.
     * This method prepares the merge context with required parameters and invokes the document merge process.
     *
     * @param disconnectionReminder The Power Supply Disconnection Reminder entity containing details of the reminder, such as ID and template ID.
     * @param generatedDocuments    The list of generated documents related to the reminder, each containing specific information like customer ID and document data.
     */
    @PSDRMergeOperation
    public void mergePSDRDocuments(PowerSupplyDisconnectionReminder disconnectionReminder, List<PowerSupplyDcnReminderDocFiles> generatedDocuments) {
        MergeContext context = MergeContext.builder()
                .disconnectionReminderId(disconnectionReminder.getId())
                .disconnectionReminderTemplateId(disconnectionReminder.getDocumentTemplateId())
                .baseDirectory(getOrCreateBaseDirectory(mergeBasePath))
                .ftpPath("%s/%s".formatted(disconnectionReminderFtpPath, LocalDate.now()))
                .generatedDocuments(generatedDocuments)
                .disconnectionReminderCustomerIds(generatedDocuments.stream().map(PowerSupplyDcnReminderDocFiles::getCustomerId).toList())
                .partitionSize(Integer.MAX_VALUE)
                .build();
        mergeDocuments(
                context,
                this::fetchAndSavePSDRFiles,
                this::savePSDRDocumentFile
        );
    }

    /**
     * Fetches and saves Power Supply DCN Reminder (PSDR) files associated with the provided reminder ID
     * and active status in the given merge context.
     * The method retrieves the list of reminder documents, processes them asynchronously,
     * and collects the results into a list of file paths.
     *
     * @param context the merge context containing information such as the reminder ID and base directory needed
     *                for processing and saving the files
     * @return a list of file paths representing the downloaded and saved PSDR files
     */
    @PSDRMergeOperation
    private List<String> fetchAndSavePSDRFiles(MergeContext context) {
        List<CompletableFuture<String>> bulkFutures = new ArrayList<>();
        log.info("Fetched {} reminder documents for disconnection reminder ID: {}", context.generatedDocuments.size(), context.disconnectionReminderId);
        for (PowerSupplyDcnReminderDocFiles document : context.generatedDocuments) {
            bulkFutures.add(CompletableFuture.supplyAsync(() -> downloadAndSavePSDRFile(document, context.baseDirectory)));
        }
        return collectDownloadedFiles(bulkFutures);
    }

    /**
     * Downloads a PSDR (Power Supply Disconnection Reminder) document from a given URL
     * and saves it to the specified directory with a structured filename.
     *
     * @param document      the document containing information such as file URL and IDs needed for downloading and naming the file
     * @param baseDirectory the base directory where the file will be saved
     * @return the absolute path of the saved file
     */
    @PSDRMergeOperation
    private String downloadAndSavePSDRFile(PowerSupplyDcnReminderDocFiles document, File baseDirectory) {
        log.info("Starting to download PSDR document file for reminder ID: {}, customer ID: {}", document.getReminderForDcnId(), document.getCustomerId());
        String downloadedFilePath = downloadAndSaveFile(
                document.getFileUrl(),
                baseDirectory.getAbsolutePath(),
                "Reminder-for-disconnection-%s-%s.pdf".formatted(document.getReminderForDcnId(), document.getCustomerId()),
                "Error downloading PSDR document file for reminder ID: %s, customer ID: %s".formatted(document.getReminderForDcnId(), document.getCustomerId())
        );
        deleteRemoteDocument(document.getFileUrl());
        return downloadedFilePath;
    }

    /**
     * Saves a Power Supply Disconnection Reminder Document file with the provided context details.
     * The method constructs a document entity using the information from the provided MergeContext
     * and persists it into the repository.
     *
     * @param context The MergeContext object containing the required details such as
     *                disconnection reminder ID, template ID, file path, customer IDs, and index.
     */
    @PSDRMergeOperation
    private void savePSDRDocumentFile(MergeContext context) {
        PowerSupplyDcnReminderDocFiles mergedReminderDocument = new PowerSupplyDcnReminderDocFiles();

        mergedReminderDocument.setReminderForDcnId(context.disconnectionReminderId);
        mergedReminderDocument.setTemplateId(context.disconnectionReminderTemplateId);
        mergedReminderDocument.setStatus(EntityStatus.ACTIVE);
        mergedReminderDocument.setFileUrl(context.filePath);
        mergedReminderDocument.setCustomerId(context.disconnectionReminderCustomerIds.get(0));
        mergedReminderDocument.setFileName(formatPSDRDocumentFileName(context.disconnectionReminderId, powerSupplyDcnReminderDocFileRepository.getNextIdValue()));

        powerSupplyDcnReminderDocFileRepository.saveAndFlush(mergedReminderDocument);
    }

    /**
     * Formats the file name for a Power Supply Disconnection Reminder document.
     * The method retrieves necessary entities such as the disconnection reminder,
     * its associated contract template, and the template details, and uses them
     * to format the document file name.
     *
     * @param disconnectionReminderId the ID of the Power Supply Disconnection Reminder
     * @param fileId the ID of the file for which the name needs to be formatted
     * @return the formatted file name for the specified reminder document
     * @throws DomainEntityNotFoundException if any of the required entities
     *         (disconnection reminder, contract template, or template details) are not found
     */
    @PSDRMergeOperation
    String formatPSDRDocumentFileName(Long disconnectionReminderId, Long fileId) {
        PowerSupplyDisconnectionReminder reminderForDcn = powerSupplyDisconnectionReminderRepository
                .findById(disconnectionReminderId)
                .orElseThrow(
                        () -> new DomainEntityNotFoundException("Can't find Reminder for disconnection with id: %s;".formatted(disconnectionReminderId))
                );
        ContractTemplate contractTemplate = contractTemplateRepository
                .findById(reminderForDcn.getDocumentTemplateId())
                .orElseThrow(
                        () -> new DomainEntityNotFoundException("Email Template with id:%s not found;".formatted(reminderForDcn.getDocumentTemplateId()))
                );
        ContractTemplateDetail templateDetail = contractTemplateDetailsRepository
                .findById(contractTemplate.getLastTemplateDetailId())
                .orElseThrow(
                        () -> new DomainEntityNotFoundException("Can't find template details with id: %s".formatted(contractTemplate.getLastTemplateDetailId()))
                );

        return reminderForDcnDocumentCreationService.formatDocumentFileName(reminderForDcn, templateDetail, null, fileId);
    }

    /**
     * Merges documents by downloading, merging, and uploading files,
     * and then saves the file data using the provided context and functions.
     *
     * @param context            the context containing information required for merging and uploading files
     * @param fetchFilesFunction a function that retrieves a list of file paths based on the provided context
     * @param saveFileConsumer   a consumer that processes and saves the context after uploading merged files
     */
    @Async
    @SneakyThrows
    public void mergeDocuments(
            MergeContext context,
            Function<MergeContext, List<String>> fetchFilesFunction,
            Consumer<MergeContext> saveFileConsumer) {
        log.info("Initializing document merging process. Context details: {}", context);
        List<String> allDownloadedFiles = fetchFilesFunction.apply(context);
        if (allDownloadedFiles == null || allDownloadedFiles.isEmpty()) {
            log.warn("No files downloaded for merging. Details: {}", context);
            return;
        } else {
            log.info("Downloaded files list: {}", allDownloadedFiles);
        }

        log.info("Downloaded {} files. Proceeding with merging.", allDownloadedFiles.size());
        List<String> mergedFiles = mergeDownloadedFiles(allDownloadedFiles, context);
        log.info("Merge completed successfully. Merged file paths: {}", mergedFiles);

        int index = 1;
        log.info("Starting file upload for merged files to FTP path: {}", context.ftpPath);
        for (String mergedFile : mergedFiles) {
            context.setIndex(index);
            String uploadedFilePath = uploadFileToFtp(mergedFile, context.ftpPath);
            context.setFilePath(uploadedFilePath);
            log.info("File uploaded successfully: {}", uploadedFilePath);

            saveFileConsumer.accept(context);
            index++;
        }
        log.info("Document merging process completed. Merged {} files.", mergedFiles.size());
    }

    /**
     * Retrieves the base directory as a File object or creates it if it does not exist.
     *
     * @param baseDirectory the path of the base directory to retrieve or create
     * @return the File object representing the base directory
     * @throws IllegalStateException if the directory does not exist and cannot be created
     */
    private File getOrCreateBaseDirectory(String baseDirectory) {
        File dir = new File(baseDirectory);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("Failed to create base directory: " + baseDirectory);
        }
        return dir;
    }

    /**
     * Uploads a file to the specified FTP directory.
     *
     * @param filePath     the local file path of the file to be uploaded
     * @param ftpDirectory the target directory on the FTP server where the file will be uploaded
     * @return the name or path of the uploaded file on the FTP server
     */
    @SneakyThrows
    private String uploadFileToFtp(String filePath, String ftpDirectory) {
        File file = new File(filePath);
        ByteMultiPartFile multipartFile = new ByteMultiPartFile(file.getName(), Files.readAllBytes(file.toPath()));
        return fileService.uploadFile(multipartFile, ftpDirectory, file.getName());
    }

    /**
     * Merges the list of downloaded files into a single list of merged files.
     * If only one file exists in a partition, it is directly added to the result.
     * Otherwise, the files in the partition are merged using the mergePdfDocuments method.
     *
     * @param allDownloadedFiles A list of file paths representing all downloaded files to be merged.
     * @param context
     * @return A list of file paths representing the merged files.
     */
    private List<String> mergeDownloadedFiles(List<String> allDownloadedFiles, MergeContext context) {
        log.info("Partitioning downloaded files with partition size: {}", context.partitionSize);
        List<String> mergedFiles = new ArrayList<>();
        List<List<String>> partitioned = ListUtils.partition(allDownloadedFiles, context.partitionSize);
        for (List<String> downloadedFiles : partitioned) {
            if (allDownloadedFiles.size() == 1) {
                log.info("Only one file found in partition. No merging required. File: {}", allDownloadedFiles.get(0));
                mergedFiles.add(allDownloadedFiles.get(0));
            } else {
                log.info("Merging files in partition: {}", downloadedFiles);
                mergedFiles.add(mergePdfDocuments(downloadedFiles));
            }
        }
        return mergedFiles;
    }

    /**
     * Merges multiple PDF documents into a single PDF file.
     *
     * @param filesToMerge a list of paths to the PDF files that need to be merged
     * @return the path to the final merged PDF file
     */
    @SneakyThrows
    public String mergePdfDocuments(List<String> filesToMerge) {
        File tempDir = new File(mergeBasePath + "tmp/");
        if (!tempDir.exists() && !tempDir.mkdir()) {
            throw new IllegalStateException("Failed to create temporary directory: " + tempDir.getAbsolutePath());
        }
        String finalDirectory = mergeBasePath + "final/";
        String finalFileName = finalDirectory + UUID.randomUUID() + ".pdf";
        File finalDir = new File(finalDirectory);
        if (!finalDir.exists() && !finalDir.mkdir()) {
            throw new IllegalStateException("Failed to create final directory: " + finalDir.getAbsolutePath());
        }

        Instant StartTimeMerge = Instant.now();
        System.out.println("final file merge count: " + filesToMerge.size());
        PDFMergerUtility ut = new PDFMergerUtility();
        for (String file : filesToMerge) {
            log.info("Merging file: {}", file);
            ut.addSource(file);
        }

        ut.setDestinationFileName(finalFileName);
        var tmp = MemoryUsageSetting.setupTempFileOnly();
        tmp.setTempDir(tempDir);
        ut.mergeDocuments(tmp);
        log.info("Merged files: {}", filesToMerge);
        long newTimeElapsed = Duration.between(StartTimeMerge, Instant.now()).toSeconds();
        System.out.println("final merge time: " + newTimeElapsed);
        for (String s : filesToMerge) {
            Path filePath = Path.of(s);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Deleted file: {}", filePath);
            } else {
                log.warn("File not found for deletion: {}", filePath);
            }
        }
        log.info("Remaining files in temp directory: {}", Arrays.stream(Objects.requireNonNull(tempDir.listFiles())).map(File::getName).toList());
        return finalFileName;
    }

    /**
     * Downloads a file from the specified download path, saves it to the target path with the given file name,
     * and returns the full path of the saved file. If an error occurs during the process, an exception is thrown
     * with the provided error message.
     *
     * @param downloadPath the source path from which the file is to be downloaded
     * @param targetPath   the destination directory where the file will be saved
     * @param fileName     the name to assign to the saved file
     * @param errorMessage the error message to log and include in the exception if an error occurs
     * @return the full path of the saved file as a String
     * @throws IllegalArgumentsProvidedException if file download or saving fails
     */
    private String downloadAndSaveFile(String downloadPath, String targetPath, String fileName, String errorMessage) {
        try {
            Path filePath = new File(targetPath, fileName).toPath();
            Files.write(
                    filePath,
                    fileService.downloadFile(downloadPath).getByteArray(),
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
            log.info("File downloaded to directory: {}", targetPath);
            log.info("File name: {}", fileName);
            log.info("Full file path: {}", filePath);
            return filePath.toString();
        } catch (Exception e) {
            log.error("Error occurred: {}. Context - Download Path: {}, Target Path: {}, File Name: {}", errorMessage, downloadPath, targetPath, fileName, e);
            throw new IllegalArgumentsProvidedException(errorMessage);
        }
    }

    /**
     * Deletes a document located at the specified remote file path.
     *
     * @param remoteFilePath the path of the remote document to be deleted.
     */
    private void deleteRemoteDocument(String remoteFilePath) {
//        fileService.deleteOnPath(remoteFilePath);
        ftpTestController.delete(remoteFilePath);
    }

    /**
     * Marks a method as belonging to a Billing Run Merge Operation and serves purely informational purposes.
     */
    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.METHOD)
    public @interface BillingRunMergeOperation {
    }

    /**
     * Indicates that a method relates to a Reminder Merge Operation, solely for organizational and descriptive purposes.
     */
    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.METHOD)
    public @interface ReminderMergeOperation {

    }

    /**
     * Denotes that a method is part of a Power Supply Disconnection Reminder (PSDR) Merge Operation for informational clarity.
     */
    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.METHOD)
    public @interface PSDRMergeOperation {
    }

    @Data
    @Builder
    public static class MergeContext {
        public Long disconnectionReminderId;
        public Long disconnectionReminderTemplateId;
        public List<Long> excludedInvoiceIds;
        public List<Long> disconnectionReminderCustomerIds;
        public List<PowerSupplyDcnReminderDocFiles> generatedDocuments;
        private int index;
        private int partitionSize;
        private Long reminderId;
        private Long processId;
        private String filePath;
        private String ftpPath;
        private File baseDirectory;
        private BillingRun billingRun;
    }
}
