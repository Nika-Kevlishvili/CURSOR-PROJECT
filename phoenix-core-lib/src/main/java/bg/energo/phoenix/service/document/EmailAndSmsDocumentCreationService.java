package bg.energo.phoenix.service.document;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.documentModels.EmailAndSmsDocumentModel;
import bg.energo.phoenix.model.documentModels.reminder.ReminderDocumentModel;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.crm.emailCommunication.EmailCommunicationAttachment;
import bg.energo.phoenix.model.entity.crm.emailCommunication.EmailCommunicationCustomerAttachment;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.entity.template.ContractTemplateDetail;
import bg.energo.phoenix.model.enums.contract.ContractType;
import bg.energo.phoenix.model.enums.documents.DocumentSigners;
import bg.energo.phoenix.model.enums.product.price.priceComponent.PriceComponentMathVariableName;
import bg.energo.phoenix.model.enums.template.ContractTemplateFileName;
import bg.energo.phoenix.repository.billing.companyDetails.CompanyDetailRepository;
import bg.energo.phoenix.repository.billing.companyDetails.CompanyLogoRepository;
import bg.energo.phoenix.repository.crm.emailCommunication.EmailCommunicationAttachmentRepository;
import bg.energo.phoenix.repository.crm.emailCommunication.EmailCommunicationCustomerAttachmentRepository;
import bg.energo.phoenix.repository.crm.emailCommunication.EmailCommunicationRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.documents.DocumentsRepository;
import bg.energo.phoenix.repository.template.ContractTemplateDetailsRepository;
import bg.energo.phoenix.repository.template.ContractTemplateRepository;
import bg.energo.phoenix.service.document.enums.DocumentPathPayloads;
import bg.energo.phoenix.service.document.enums.FileFormat;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.service.signing.SignerChainManager;
import bg.energo.phoenix.util.epb.EPBJsonUtils;
import bg.energo.phoenix.util.template.document.DocumentGenerationUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

import static bg.energo.phoenix.exception.ErrorCode.APPLICATION_ERROR;
import static bg.energo.phoenix.service.document.DocumentParserService.parseDocxToHtml;

@Slf4j
@Service
public class EmailAndSmsDocumentCreationService extends AbstractDocumentCreationService {

    private static final String FOLDER_PATH = "mass_communication_documents";
    private final EmailCommunicationRepository emailCommunicationRepository;
    private final EmailCommunicationCustomerAttachmentRepository emailCommunicationCustomerAttachmentRepository;
    private final EmailCommunicationAttachmentRepository emailCommunicationAttachmentRepository;
    private final CustomerRepository customerRepository;

    public EmailAndSmsDocumentCreationService(
            ContractTemplateDetailsRepository contractTemplateDetailsRepository,
            ContractTemplateRepository contractTemplateRepository,
            DocumentGenerationService documentGenerationService,
            DocumentGenerationUtil documentGenerationUtil,
            DocumentsRepository documentsRepository,
            CompanyDetailRepository companyDetailRepository,
            CompanyLogoRepository companyLogoRepository,
            SignerChainManager signerChainManager,
            FileService fileService,
            EmailCommunicationRepository emailCommunicationRepository,
            EmailCommunicationCustomerAttachmentRepository emailCommunicationCustomerAttachmentRepository,
            EmailCommunicationAttachmentRepository emailCommunicationAttachmentRepository,
            CustomerRepository customerRepository
    ) {
        super(
                contractTemplateDetailsRepository,
                contractTemplateRepository,
                documentGenerationService,
                documentGenerationUtil,
                documentsRepository,
                companyDetailRepository,
                companyLogoRepository,
                signerChainManager,
                fileService
        );
        this.emailCommunicationRepository = emailCommunicationRepository;
        this.emailCommunicationCustomerAttachmentRepository = emailCommunicationCustomerAttachmentRepository;
        this.emailCommunicationAttachmentRepository = emailCommunicationAttachmentRepository;
        this.customerRepository = customerRepository;
    }

    /**
     * Generates an EmailAndSmsDocumentModel from the given request, enriching it with data
     * fetched from various repositories based on customer and contract details.
     * The model is filled with shared customer information, contract details, liability data, price components, etc.
     *
     * @param request The request object containing parameters such as contract and customer details.
     * @return A populated EmailAndSmsDocumentModel based on the provided request.
     */
    public EmailAndSmsDocumentModel generateDocumentJsonModel(EmailAndSmsDocumentRequest request) {
        EmailAndSmsDocumentModel emailAndSmsDocumentModel = buildModelWithSharedInfo(new EmailAndSmsDocumentModel());
        EmailAndSmsDocumentModel.CustomerAdditionalInfoProjection customerAdditionalInfoProjection;

        if (request.getEmailCommunicationCustomerId() != null && request.getSmsCommunicationCustomerId() != null) {
            throw new IllegalArgumentException("Only one of the customer ID parameters can be provided.");
        }

        if (request.getEmailCommunicationCustomerId() != null) {
            customerAdditionalInfoProjection = emailCommunicationRepository.fetchCustomerAdditionalInfoFromEmail(
                    request.getEmailCommunicationCustomerId()
            );
        } else {
            customerAdditionalInfoProjection = emailCommunicationRepository.fetchCustomerAdditionalInfoFromSms(
                    request.getSmsCommunicationCustomerId()
            );
        }

        if (Objects.nonNull(customerAdditionalInfoProjection)) {
            emailAndSmsDocumentModel.fillWithCustomerAdditionalInfo(customerAdditionalInfoProjection);
            Optional<String> managersOptional = emailCommunicationRepository.findManagersByCustomerDetailId(
                    customerAdditionalInfoProjection.getCustomerDetailId()
            );
            managersOptional.ifPresent(
                    s -> emailAndSmsDocumentModel.Managers = EPBJsonUtils.readList(
                            s,
                            EmailAndSmsDocumentModel.Manager.class
                    )
            );

            Map<String, Object> liabilityData = emailCommunicationRepository.findLiabilityDataBasedOnCustomer(
                    customerAdditionalInfoProjection.getCustomerId()
            );
            if (Objects.nonNull(liabilityData)) {
                emailAndSmsDocumentModel.fillWithLiabilityData(liabilityData);
            }
        }

        if (Objects.nonNull(request.getContractDetailId()) && Objects.nonNull(request.getContractNumber())) {
            EmailAndSmsDocumentModel.ContractsInfoProjection contractsInfoProjection = emailCommunicationRepository.fetchContractInfoByContractDetailIdAndContractNumber(
                    request.getContractDetailId(),
                    request.getContractNumber()
            );
            if (Objects.nonNull(contractsInfoProjection)) {
                emailAndSmsDocumentModel.fillWithContractInfo(contractsInfoProjection);
            }
        }

        if (ContractType.SERVICE_CONTRACT.equals(request.getContractType())) {
            Optional<String> contractPodsOptional = emailCommunicationRepository.fetchServiceContractPodsByContractId(request.getContractDetailId());
            contractPodsOptional.ifPresent(
                    s -> emailAndSmsDocumentModel.ContractPODs = EPBJsonUtils.readList(
                            s,
                            EmailAndSmsDocumentModel.POD.class
                    )
            );
        } else if (ContractType.PRODUCT_CONTRACT.equals(request.getContractType())) {
            Optional<String> contractPodsOptional = emailCommunicationRepository.fetchProductContractPodsByContractDetailId(request.getContractDetailId());
            contractPodsOptional.ifPresent(
                    s -> emailAndSmsDocumentModel.ContractPODs = EPBJsonUtils.readList(
                            s,
                            EmailAndSmsDocumentModel.POD.class
                    )
            );
        }

        List<String> priceComponentTags = null;
        if (ContractType.SERVICE_CONTRACT.equals(request.getContractType())) {
            priceComponentTags = emailCommunicationRepository.fetchPriceComponentTagsService(request.getContractDetailId());
        } else if (ContractType.PRODUCT_CONTRACT.equals(request.getContractType())) {
            priceComponentTags = emailCommunicationRepository.fetchPriceComponentTagsProduct(request.getContractDetailId());
        }

        if (CollectionUtils.isNotEmpty(priceComponentTags)) {
            List<EmailAndSmsDocumentModel.XField> xFields = new ArrayList<>();
            priceComponentTags.forEach(s -> {
                List<EmailAndSmsDocumentModel.XFieldVariable> xFieldVariables = EPBJsonUtils.readList(
                        s,
                        EmailAndSmsDocumentModel.XFieldVariable.class
                );
                EmailAndSmsDocumentModel.XField xField = new EmailAndSmsDocumentModel.XField();
                xFieldVariables.forEach(xFieldVariable -> {
                    xField.TAG = xFieldVariable.tag;

                    if (PriceComponentMathVariableName.X1.name().equals(xFieldVariable.variableName)) {
                        xField.X1Value = xFieldVariable.value;
                        xField.X1Desc = xFieldVariable.description;
                    } else if (PriceComponentMathVariableName.X2.name().equals(xFieldVariable.variableName)) {
                        xField.X2Value = xFieldVariable.value;
                        xField.X2Desc = xFieldVariable.description;
                    } else if (PriceComponentMathVariableName.X3.name().equals(xFieldVariable.variableName)) {
                        xField.X3Value = xFieldVariable.value;
                        xField.X3Desc = xFieldVariable.description;
                    } else if (PriceComponentMathVariableName.X4.name().equals(xFieldVariable.variableName)) {
                        xField.X4Value = xFieldVariable.value;
                        xField.X4Desc = xFieldVariable.description;
                    } else if (PriceComponentMathVariableName.X5.name().equals(xFieldVariable.variableName)) {
                        xField.X5Value = xFieldVariable.value;
                        xField.X5Desc = xFieldVariable.description;
                    } else if (PriceComponentMathVariableName.X6.name().equals(xFieldVariable.variableName)) {
                        xField.X6Value = xFieldVariable.value;
                        xField.X6Desc = xFieldVariable.description;
                    } else if (PriceComponentMathVariableName.X7.name().equals(xFieldVariable.variableName)) {
                        xField.X7Value = xFieldVariable.value;
                        xField.X7Desc = xFieldVariable.description;
                    } else if (PriceComponentMathVariableName.X8.name().equals(xFieldVariable.variableName)) {
                        xField.X8Value = xFieldVariable.value;
                        xField.X8Desc = xFieldVariable.description;
                    }
                });

                xFields.add(xField);
            });
            emailAndSmsDocumentModel.X = xFields;
        }

        if (Objects.nonNull(request.getContractNumber())) {
            EmailAndSmsDocumentModel.ContractsActionsProjection contractsActionsProjection = emailCommunicationRepository.fetchContractActionsByContractNumber(
                    request.getContractNumber()
            );

            if (Objects.nonNull(contractsActionsProjection)) {
                emailAndSmsDocumentModel.fillWithContractActionsInfo(contractsActionsProjection);

                if (Objects.nonNull(contractsActionsProjection.getActionId())) {
                    Optional<String> actionGoPodsOptional = emailCommunicationRepository.findActionGoPodsByActionId(contractsActionsProjection.getActionId());
                    actionGoPodsOptional.ifPresent(
                            s -> emailAndSmsDocumentModel.ActionTerminationGOPODs = EPBJsonUtils.readList(
                                    s,
                                    EmailAndSmsDocumentModel.ActionTerminationGO.class
                            )
                    );
                }

                if (Objects.nonNull(contractsActionsProjection.getActionId())) {
                    List<EmailAndSmsDocumentModel.ActionTerminationGOListPODProjection> actionGoPodsList = emailCommunicationRepository.findActionGoListPodsByActionId(
                            contractsActionsProjection.getActionId()
                    );
                    emailAndSmsDocumentModel.ActionTerminationGOListPOD = actionGoPodsList
                            .stream()
                            .map(EmailAndSmsDocumentModel.ActionTerminationGOListPOD::new)
                            .toList();
                }

            }
        }

        return emailAndSmsDocumentModel;
    }

    /**
     * Generates and saves an email attachment for a customer, based on the provided template and document model.
     * This method generates the email attachment in PDF format.
     * The process includes:
     * 1. Retrieving the contract template details using the provided template ID.
     * 2. Calling the `generateEmailAttachments` method to generate the email attachment.
     * 3. Logging the result and any errors encountered during the process.
     *
     * @param templateId               The ID of the contract template used for generating the attachment.
     * @param customerDetailId         The ID of the customer whose email attachment is being generated.
     * @param emailCommunicationId     The ID associated with the email communication.
     * @param emailAndSmsDocumentModel The model containing the data to populate the email attachment.
     * @throws RuntimeException If an error occurs during the email attachment generation process.
     */
    public void generateAndSaveEmailAttachment(
            Long templateId,
            Long customerDetailId,
            Long emailCommunicationId,
            EmailAndSmsDocumentModel emailAndSmsDocumentModel
    ) {
        try {
            log.info(
                    "Generating email attachment for customerDetailId: {} and emailCommunicationId: {}",
                    customerDetailId,
                    emailCommunicationId
            );
            ContractTemplateDetail templateDetail = getContractTemplateLastDetails(templateId);
            log.debug("Found contract template detail: {}", templateDetail);
            String fileName = formatDocumentFileName(
                    templateDetail,
                    customerDetailId
            );
            log.debug("Generated file name: {}", fileName);
            ByteArrayResource templateFileResource = createTemplateFileResource(templateDetail);
            log.debug("Template file resource created for template ID: {}", templateDetail.getId());
            DocumentPathPayloads documentPathPayloads = documentGenerationService.generateDocument(
                    templateFileResource,
                    buildFileDestinationPath(),
                    fileName,
                    emailAndSmsDocumentModel,
                    Set.of(FileFormat.PDF),
                    false
            );
            log.debug(
                    "Document generated successfully. Paths: PDF - {}, DOCX - {}",
                    documentPathPayloads.pdfPath(),
                    documentPathPayloads.docXPath()
            );

            Document document = signAttachments(templateDetail, FileFormat.PDF, fileName, documentPathPayloads);
            emailCommunicationAttachmentRepository.save(
                    EmailCommunicationAttachment
                            .builder()
                            .name(fileName)
                            .fileUrl(document.getSignedFileUrl())
                            .status(EntityStatus.ACTIVE)
                            .emailCommunicationId(emailCommunicationId)
                            .build()
            );
            log.info("Successfully generated email attachment for emailCommunicationId: {}", emailCommunicationId);
        } catch (Exception e) {
            log.error("Error occurred while generating email attachment for emailCommunicationId: {}", emailCommunicationId, e);
            throw new RuntimeException("Error occurred while generating email attachment.", e);
        }
    }

    public void generateAndSaveEmailCustomerAttachment(
            Long templateId,
            Long customerDetailId,
            Long emailCommunicationCustomerId,
            EmailAndSmsDocumentModel emailAndSmsDocumentModel
    ) {
        try {
            log.info("Generating email attachment for customerDetailId: {}", customerDetailId);
            ContractTemplateDetail templateDetail = getContractTemplateLastDetails(templateId);
            String fileName = formatDocumentFileName(
                    templateDetail,
                    customerDetailId
            );
            log.debug("Generated file name: {}", fileName);
            ByteArrayResource templateFileResource = createTemplateFileResource(templateDetail);
            log.debug("Template file resource created for template ID: {}", templateDetail.getId());
            DocumentPathPayloads documentPathPayloads = documentGenerationService.generateDocument(
                    templateFileResource,
                    buildFileDestinationPath(),
                    fileName,
                    emailAndSmsDocumentModel,
                    Set.of(FileFormat.PDF),
                    false
            );
            Document document = signAttachments(templateDetail, FileFormat.PDF, fileName, documentPathPayloads);
            emailCommunicationCustomerAttachmentRepository.save(
                    EmailCommunicationCustomerAttachment
                            .builder()
                            .name(fileName)
                            .fileUrl(document.getSignedFileUrl())
                            .status(EntityStatus.ACTIVE)
                            .emailCommunicationCustomerId(emailCommunicationCustomerId)
                            .build()
            );
            log.info("Attachment successfully saved for emailCommunicationCustomerId: {}", emailCommunicationCustomerId);
        } catch (Exception e) {
            log.error("Error occurred while generating email attachment for customerDetailId: {}", customerDetailId, e);
            throw new RuntimeException("Error occurred while generating email attachment.", e);
        }
    }

    /**
     * Generates the email body by processing the contract template and the provided document model.
     * This method:
     * 1. Creates a ByteArrayResource from the template file.
     * 2. Generates a DOCX byte array resource based on the document model.
     * 3. Parses the DOCX content into an HTML format for the email body.
     *
     * @param templateDetail The details of the contract template.
     * @param documentModel  The model containing the data to populate the template.
     * @return The generated email body in HTML format.
     * @throws ClientException If an error occurs during the template resource creation, document generation, or parsing.
     */
    public String generateEmailCustomerBody(
            ContractTemplateDetail templateDetail,
            EmailAndSmsDocumentModel documentModel
    ) {
        try {
            ByteArrayResource templateFileResource = createTemplateFileResource(templateDetail);
            log.info("Template file resource created for template ID: {}", templateDetail.getId());
            byte[] resource = generateDocxByteResourceForEmailBody(templateFileResource, documentModel);
            log.info("Generated DOCX byte array for email body.");
            String emailBody;
            try {
                emailBody = parseDocxToHtml(resource);
                log.info("Successfully parsed DOCX to HTML email body.");
            } catch (Exception e) {
                log.error("Error parsing DOCX to HTML for email body.", e);
                throw new ClientException("Can't parse docx", ErrorCode.APPLICATION_ERROR);
            }
            return emailBody;
        } catch (Exception e) {
            log.error("Exception occurred while generating email body for template: {}", templateDetail.getId(), e);
            throw new ClientException("Exception while storing template file: %s".formatted(e.getMessage()), APPLICATION_ERROR);
        }
    }

    /**
     * Generates a single email body by processing the contract template and the provided document model.
     * The process involves the following steps:
     * 1. Retrieving the contract template details for the given template ID.
     * 2. Creating a ByteArrayResource from the template file.
     * 3. Generating a DOCX byte array resource based on the document model.
     * 4. Parsing the DOCX content into an HTML format for the email body.
     *
     * @param templateId    The ID of the template used to generate the email body.
     * @param documentModel The model containing the data to populate the template.
     * @return The generated email body in HTML format.
     * @throws ClientException If there is an error during template retrieval, document generation, or parsing.
     */
    public String generateSingleEmailBody(
            Long templateId,
            EmailAndSmsDocumentModel documentModel
    ) {
        log.info("Starting email body generation for template ID: {}", templateId);
        try {
            ContractTemplateDetail templateDetail = getContractTemplateLastDetails(templateId);
            log.info("Successfully retrieved contract template details for template ID: {}", templateId);
            ByteArrayResource templateFileResource = createTemplateFileResource(templateDetail);
            log.info("Template file resource created for template ID: {}", templateId);
            byte[] resource = generateDocxByteResourceForEmailBody(templateFileResource, documentModel);
            log.info("Generated DOCX byte array for email body.");
            String emailBody;
            try {
                emailBody = parseDocxToHtml(resource);
                log.info("Successfully parsed DOCX to HTML email body.");
            } catch (Exception e) {
                log.error("Error parsing DOCX to HTML for email body.", e);
                throw new ClientException("Can't parse docx", ErrorCode.APPLICATION_ERROR);
            }
            return emailBody;
        } catch (Exception e) {
            log.error("Exception occurred while generating email body for template ID: {}. Error: {}", templateId, e.getMessage(), e);
            throw new ClientException("Exception while storing template file: %s".formatted(e.getMessage()), APPLICATION_ERROR);
        }
    }

    /**
     * Generates a single email body for a reminder by processing the contract template
     * and the provided document model.
     * The process involves the following steps:
     * 1. Retrieving the contract template details for the given template ID.
     * 2. Creating a ByteArrayResource from the template file.
     * 3. Generating a DOCX byte array resource based on the document model.
     * 4. Parsing the DOCX content into an HTML format for email body.
     * 5. Returning the parsed email body along with the subject of the template.
     *
     * @param templateId    The ID of the template used for the reminder.
     * @param documentModel The model containing the data to populate the template.
     * @return A {@link Pair} containing the email body (HTML format) and the email subject.
     * @throws ClientException If there is an error during template retrieval, document generation, or parsing.
     */
    public Pair<String, String> generateSingleEmailBodyForReminder(
            Long templateId,
            ReminderDocumentModel documentModel
    ) {
        log.info("Starting email body generation for reminder with template ID: {}", templateId);
        try {
            ContractTemplateDetail templateDetail = getContractTemplateLastDetails(templateId);
            log.info("Retrieved contract template details for template ID: {}", templateId);
            ByteArrayResource templateFileResource = createTemplateFileResource(templateDetail);
            log.info("Template file resource created successfully for template ID: {}", templateId);
            byte[] resource = generateDocxByteResourceForEmailBody(templateFileResource, documentModel);
            log.info("Generated DOCX byte array for email body.");
            String emailBody;
            try {
                emailBody = parseDocxToHtml(resource);
                log.info("Successfully parsed DOCX to HTML email body.");
            } catch (Exception e) {
                log.error("Error parsing DOCX file to HTML for email body.", e);
                throw new ClientException("Can't parse docx", ErrorCode.APPLICATION_ERROR);
            }
            return Pair.of(emailBody, templateDetail.getSubject());
        } catch (Exception e) {
            log.error("Exception occurred while generating email body for reminder: {}", e.getMessage(), e);
            throw new ClientException("Exception while storing template file: %s".formatted(e.getMessage()), APPLICATION_ERROR);
        }
    }

    /**
     * Generates a DOCX file byte array resource for email body by using a template and the provided document model.
     * This method performs the following steps:
     * 1. Uses a template and the provided model to generate a document.
     * 2. Downloads the generated document file as a byte array.
     * 3. Returns the byte array representing the document.
     *
     * @param templateFileResource The template file as a {@link ByteArrayResource}.
     * @param documentModel        The document model containing data to populate the template.
     * @param <T>                  The type of the document model.
     * @return A byte array representing the generated DOCX document.
     * @throws Exception If an error occurs during document generation or file download.
     */
    private <T> byte[] generateDocxByteResourceForEmailBody(
            ByteArrayResource templateFileResource,
            T documentModel
    ) throws Exception {
        log.info("Starting DOCX document generation for email body.");
        try {
            DocumentPathPayloads documentPathPayloads = documentGenerationService.generateDocument(
                    templateFileResource,
                    buildFileDestinationPath(),
                    UUID.randomUUID().toString(),
                    documentModel,
                    Set.of(FileFormat.DOCX),
                    false
            );
            log.info("Document generated successfully. Document path: {}", documentPathPayloads.docXPath());
            ByteArrayResource byteArrayResource = fileService.downloadFile(documentPathPayloads.docXPath());
            log.info("Document file downloaded successfully. File size: {} bytes", byteArrayResource.getByteArray().length);
            return byteArrayResource.getByteArray();
        } catch (Exception e) {
            log.error("Error occurred during DOCX document generation or file download.", e);
            throw e;
        }
    }

    protected String formatDocumentFileName(
            ContractTemplateDetail contractTemplateDetail,
            Long customerDetailId
    ) {
        log.debug("Starting file name formatting");
        List<String> fileNameParts = new ArrayList<>();
        log.debug("Extracting file prefix");
        fileNameParts.add(documentGenerationUtil.extractFilePrefix(contractTemplateDetail));
        log.debug("Extracting file name");
        fileNameParts.add(extractFileName(contractTemplateDetail, customerDetailId));
        log.debug("Extracting file suffix");
        fileNameParts.add(extractFileSuffix(LocalDateTime.now(), contractTemplateDetail));
        fileNameParts.removeIf(StringUtils::isBlank);

        String fileParts = String.join("_", fileNameParts);
        fileParts = StringUtils.isBlank(fileParts) ? UUID.randomUUID()
                .toString() : fileParts;

        return fileParts
                .substring(0, Math.min(fileParts.length(), 200))
                .concat(".")
                .concat(FileFormat.PDF.getSuffix())
                .replaceAll("/", "_");
    }

    public String extractFileName(ContractTemplateDetail contractTemplateDetail, Long customerDetailId) {
        try {
            List<ContractTemplateFileName> fileName = contractTemplateDetail
                    .getFileName()
                    .stream()
                    .sorted(Comparator.comparing(ContractTemplateFileName::getPriority))
                    .toList();

            if (CollectionUtils.isEmpty(fileName)) {
                return UUID.randomUUID()
                        .toString();
            } else {
                Map<Long, Pair<Customer, CustomerDetails>> customerCache = new HashMap<>();
                List<String> nameParts = new ArrayList<>();

                for (ContractTemplateFileName contractTemplateFileName : fileName) {
                    switch (contractTemplateFileName) {
                        case CUSTOMER_IDENTIFIER -> {
                            String identifier = customerRepository.getCustomerIdentifierByCustomerDetailId(customerDetailId);
                            nameParts.add(identifier);
                        }
                        case CUSTOMER_NAME -> {
                            Pair<Customer, CustomerDetails> customerPair = documentGenerationUtil.getCustomer(
                                    customerDetailId,
                                    customerCache
                            );
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
                            Pair<Customer, CustomerDetails> customerPair = documentGenerationUtil.getCustomer(
                                    customerDetailId,
                                    customerCache
                            );
                            Customer customer = customerPair.getKey();

                            nameParts.add(String.valueOf(customer.getCustomerNumber()));
                        }
                        case DOCUMENT_NUMBER -> nameParts.add(UUID.randomUUID()
                                .toString());
                        case FILE_ID -> nameParts.add("");
                        case TIMESTAMP -> nameParts.add(LocalDateTime.now()
                                .toString());
                    }
                }

                return String.join("_", nameParts);
            }
        } catch (Exception e) {
            log.error("Exception handled while trying to extract file name", e);
            return UUID.randomUUID()
                    .toString();
        }
    }

    private Document signAttachments(
            ContractTemplateDetail templateDetail,
            FileFormat fileFormat,
            String fileName,
            DocumentPathPayloads documentPathPayloads
    ) {
        String fileUrl = fileFormat.equals(FileFormat.PDF) ? documentPathPayloads.pdfPath() : documentPathPayloads.docXPath();
        log.debug("Selected file URL: {}", fileUrl);

        List<DocumentSigners> documentSigners = getDocumentSigners(templateDetail);
        Document document = saveDocument(
                fileName,
                fileUrl,
                fileFormat,
                documentSigners,
                templateDetail.getTemplateId()
        );

        if (FileFormat.PDF.equals(fileFormat)) {
            signerChainManager.startSign(List.of(document));
        }

        return document;
    }

    @Override
    protected String folderPath() {
        return FOLDER_PATH;
    }
}
