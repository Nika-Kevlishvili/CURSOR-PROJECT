package bg.energo.phoenix.util.template.document;

import bg.energo.mass_comm.models.Attachment;
import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.IllegalArgumentsProvidedException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.invoice.Invoice;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.template.ContractTemplateDetail;
import bg.energo.phoenix.model.entity.template.ContractTemplateFiles;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStatus;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.template.ContractTemplateFileName;
import bg.energo.phoenix.model.enums.template.ContractTemplateSuffix;
import bg.energo.phoenix.model.response.proxy.FileContent;
import bg.energo.phoenix.repository.billing.invoice.InvoiceDocumentRepository;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.template.ContractTemplateFileRepository;
import bg.energo.phoenix.service.document.enums.DocumentPathPayloads;
import bg.energo.phoenix.service.document.enums.FileFormat;
import bg.energo.phoenix.service.document.ftpService.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static bg.energo.phoenix.exception.ErrorCode.APPLICATION_ERROR;

@RequiredArgsConstructor
@Service
@Slf4j
public class DocumentGenerationUtil {
    private final CustomerRepository customerRepository;
    private final CustomerDetailsRepository customerDetailsRepository;
    private final ContractTemplateFileRepository contractTemplateFileRepository;
    private final FileService fileService;
    private final InvoiceDocumentRepository invoiceDocumentRepository;

    /**
     * Formats the file name for a generated document based on the provided invoice and contract template details.
     *
     * @param invoice                The invoice for which the document is being generated.
     * @param contractTemplateDetail The contract template details used to generate the document.
     * @return The formatted file name for the generated document.
     */
    public String formatInvoiceFileName(Invoice invoice, ContractTemplateDetail contractTemplateDetail) {
        log.debug("Starting file name formatting");
        List<String> fileNameParts = new ArrayList<>();
        log.debug("Extracting file prefix");
        fileNameParts.add(extractFilePrefix(contractTemplateDetail));
        log.debug("Extracting file name");
        fileNameParts.add(extractFileName(invoice, contractTemplateDetail));
        log.debug("Extracting file suffix");
        fileNameParts.add(extractFileSuffix(invoice, contractTemplateDetail));
        fileNameParts.removeIf(StringUtils::isBlank);

        String fileParts = String.join("_", fileNameParts);
        fileParts = StringUtils.isBlank(fileParts) ? "Invoice" : fileParts;
        return fileParts.substring(0, Math.min(fileParts.length(), 200)).concat(".").concat(FileFormat.PDF.getSuffix()).replaceAll("/", "_");
    }

    /**
     * Retrieves the local file path of the contract template file associated with the provided contract template details.
     * <p>
     * This method downloads the contract template file from the FTP location specified in the contract template details, and
     * creates a temporary file with the downloaded content. The absolute path of the temporary file is returned.
     *
     * @param contractTemplateDetail The contract template details containing the information about the template file to retrieve.
     * @return The absolute path of the temporary file containing the downloaded contract template.
     * @throws IllegalArgumentsProvidedException If the contract template file with the specified ID is not found or has been deleted.
     * @throws ClientException                   If an error occurs while downloading the contract template file.
     */
    public String getTemplateFileLocalPath(ContractTemplateDetail contractTemplateDetail) {

        ContractTemplateFiles contractTemplateFile = contractTemplateFileRepository
                .findByIdAndStatus(contractTemplateDetail.getTemplateFileId(), EntityStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentsProvidedException("Contract Template File with id: [%s] not found or DELETED;".formatted(contractTemplateDetail.getTemplateFileId())));

        String templateFileLocalPath;
        try {
            String fileUrl = contractTemplateFile.getFileUrl();
            log.debug("Start downloading template from FTP path: [%s]".formatted(fileUrl));
            ByteArrayResource templateFileResource = fileService.downloadFile(fileUrl);
            Path tempFile = Files.createTempFile(UUID.randomUUID().toString(), fileUrl.substring(fileUrl.lastIndexOf('.')));
            try {
                Files.write(tempFile, templateFileResource.getByteArray());
                templateFileLocalPath = tempFile.toAbsolutePath().toString();
                log.debug("Template downloaded, local path: [%s]".formatted(templateFileLocalPath));
            } catch (Exception e) {
                throw new ClientException("Exception while reading file: [%s]".formatted(e.getMessage()), APPLICATION_ERROR);
            }
        } catch (Exception e) {
            throw new ClientException("Error handled while trying to download template file;", APPLICATION_ERROR);
        }

        return templateFileLocalPath;
    }

    /**
     * Extracts the file name suffix for a generated document based on the provided invoice and contract template details.
     *
     * @param invoice                The invoice for which the document is being generated.
     * @param contractTemplateDetail The contract template details used to generate the document.
     * @return The formatted file name suffix for the generated document, or an empty string if the file name suffix is not configured.
     */
    private String extractFileSuffix(Invoice invoice, ContractTemplateDetail contractTemplateDetail) {
        try {
            LocalDateTime createDate = invoice.getCreateDate();
            ContractTemplateSuffix fileNameSuffix = contractTemplateDetail.getFileNameSuffix();
            if (Objects.isNull(fileNameSuffix)) {
                return "";
            }

            return DateTimeFormatter.ofPattern(fileNameSuffix.getFormat()).format(createDate);
        } catch (Exception e) {
            log.error("Exception handled while trying to extract file suffix", e);
            return "";
        }
    }

    /**
     * Extracts the file name for a generated document based on the provided invoice and contract template details.
     *
     * @param invoice                The invoice for which the document is being generated.
     * @param contractTemplateDetail The contract template details used to generate the document.
     * @return The formatted file name for the generated document.
     */
    private String extractFileName(Invoice invoice, ContractTemplateDetail contractTemplateDetail) {
        String documentNumber = invoice.getInvoiceStatus() == InvoiceStatus.CANCELLED ? invoice.getInvoiceCancellationNumber() : invoice.getInvoiceNumber();
        try {
            List<ContractTemplateFileName> fileName = contractTemplateDetail
                    .getFileName()
                    .stream()
                    .sorted(Comparator.comparing(ContractTemplateFileName::getPriority))
                    .toList();

            if (CollectionUtils.isEmpty(fileName)) {
                return documentNumber;
            } else {
                Map<Long, Pair<Customer, CustomerDetails>> customerCache = new HashMap<>();
                List<String> nameParts = new ArrayList<>();

                for (ContractTemplateFileName contractTemplateFileName : fileName) {
                    switch (contractTemplateFileName) {
                        case CUSTOMER_IDENTIFIER -> {
                            String identifier = customerRepository.getCustomerIdentifierByCustomerDetailId(invoice.getCustomerDetailId());
                            nameParts.add(identifier);
                        }
                        case CUSTOMER_NAME -> {
                            Pair<Customer, CustomerDetails> customerPair = getCustomer(invoice.getCustomerDetailId(), customerCache);
                            Customer customer = customerPair.getKey();
                            CustomerDetails customerDetails = customerPair.getValue();

                            switch (customer.getCustomerType()) {
                                case LEGAL_ENTITY -> {
                                    String customerName = "%s_%s".formatted(
                                            customerDetails.getName(),
                                            "LEGAL_ENTITY"
                                    );
                                    nameParts.add(customerName.substring(0, Math.min(customerName.length(), 64)));
                                }
                                case PRIVATE_CUSTOMER -> {
                                    String customerName = "%s_%s_%s".formatted(
                                            customerDetails.getName(),
                                            customerDetails.getMiddleName(),
                                            customerDetails.getLastName()
                                    );
                                    nameParts.add(customerName.substring(0, Math.min(customerName.length(), 64)));
                                }
                            }
                        }
                        case CUSTOMER_NUMBER -> {
                            Pair<Customer, CustomerDetails> customerPair = getCustomer(invoice.getCustomerDetailId(), customerCache);
                            Customer customer = customerPair.getKey();

                            nameParts.add(String.valueOf(customer.getCustomerNumber()));
                        }
                        case DOCUMENT_NUMBER -> nameParts.add(documentNumber);
                        case FILE_ID -> nameParts.add(String.valueOf(invoiceDocumentRepository.getNextIdValue()));
                        case TIMESTAMP -> nameParts.add(invoice.getCreateDate().toString());
                    }
                }

                return String.join("_", nameParts);
            }
        } catch (Exception e) {
            log.error("Exception handled while trying to extract file name", e);
            return documentNumber;
        }
    }

    /**
     * Retrieves a customer and their associated customer details from a cache, or fetches them from the database if not found in the cache.
     *
     * @param customerDetailId the ID of the customer details to retrieve
     * @param customerCache    a cache of customer and customer details pairs, keyed by customer detail ID
     * @return a pair containing the customer and their customer details
     * @throws DomainEntityNotFoundException if the customer or customer details are not found
     */
    public Pair<Customer, CustomerDetails> getCustomer(Long customerDetailId, Map<Long, Pair<Customer, CustomerDetails>> customerCache) {
        if (!customerCache.containsKey(customerDetailId)) {
            Customer customer = customerRepository
                    .findByCustomerDetailIdAndStatusIn(customerDetailId, Arrays.stream(CustomerStatus.values()).toList())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Customer with detail id: [%s] not found;".formatted(customerDetailId)));
            CustomerDetails customerDetails = customerDetailsRepository.findById(customerDetailId)
                    .orElseThrow(() -> new DomainEntityNotFoundException("Customer with detail id: [%s] not found;".formatted(customerDetailId)));

            customerCache.put(customerDetailId, Pair.of(customer, customerDetails));
        }

        return Pair.of(customerCache.get(customerDetailId).getKey(), customerCache.get(customerDetailId).getValue());
    }

    /**
     * Extracts the file name prefix from the provided contract template detail.
     *
     * @param contractTemplateDetail the contract template detail to extract the file name prefix from
     * @return the file name prefix, or an empty string if the prefix is null
     */
    public String extractFilePrefix(ContractTemplateDetail contractTemplateDetail) {
        return ObjectUtils.defaultIfNull(contractTemplateDetail.getFileNamePrefix(), "");
    }

    /**
     * Retrieves the file path for a document based on the specified format.
     *
     * @param format the format of the document (PDF, DOCX, or XLSX)
     * @param documentPathPayloads the payload containing the file paths for each format
     * @return the file path for the document, or null if the format is not supported
     */
    public String getGeneratedDocumentPath(FileFormat format, DocumentPathPayloads documentPathPayloads) {
        switch (format) {
            case PDF -> {
                return documentPathPayloads.pdfPath();
            }
            case DOCX -> {
                return documentPathPayloads.docXPath();
            }
            case XLSX -> {
                return documentPathPayloads.xlsxPath();
            }
        }
        return null;
    }

    /**
     * Extracts a file suffix based on the creation date and contract template details.
     *
     * @param createDate the date to be formatted according to the template suffix pattern
     * @param contractTemplateDetail the contract template containing suffix formatting rules
     * @return a formatted string representing the file suffix, or empty string if template suffix is null or in case of errors
     */
    public String extractFileSuffix(LocalDateTime createDate, ContractTemplateDetail contractTemplateDetail) {
        try {
            ContractTemplateSuffix fileNameSuffix = contractTemplateDetail.getFileNameSuffix();
            if (Objects.isNull(fileNameSuffix)) {
                return "";
            }

            return DateTimeFormatter.ofPattern(fileNameSuffix.getFormat()).format(createDate);
        } catch (Exception e) {
            log.error("Exception handled while trying to extract file suffix", e);
            return "";
        }
    }

    /**
     * Converts a FileContent object to an Attachment object.
     *
     * @param fileContent the source file content containing the file name and binary content
     * @return an Attachment object with content type derived from the file name, along with the original file name, size, and content
     */
    public Attachment convertFileContentToAttachment(FileContent fileContent) {
        return new Attachment(
                fileContent.getFileName(),
                URLConnection.guessContentTypeFromName(fileContent.getFileName()),
                fileContent.getContent().length,
                fileContent.getContent()
        );
    }
}
