package bg.energo.phoenix.service.document;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.documentModels.disconnectionPowerSupplyRequests.DisconnectionPowerSupplyRequestsDocumentModel;
import bg.energo.phoenix.model.documentModels.disconnectionPowerSupplyRequests.DisconnectionPowerSupplyRequestsDocumentPodImpl;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.entity.nomenclature.product.GridOperator;
import bg.energo.phoenix.model.entity.receivable.disconnectionPowerSupplyRequests.DisconnectionOfPowerSupplyRequestTemplate;
import bg.energo.phoenix.model.entity.receivable.disconnectionPowerSupplyRequests.DisconnectionPowerSupplyRequests;
import bg.energo.phoenix.model.entity.receivable.disconnectionPowerSupplyRequests.DisconnectionPowerSupplyRequestsDocuments;
import bg.energo.phoenix.model.entity.template.ContractTemplateDetail;
import bg.energo.phoenix.model.enums.documents.DocumentSigners;
import bg.energo.phoenix.model.enums.documents.DocumentStatus;
import bg.energo.phoenix.model.enums.template.ContractTemplateFileFormat;
import bg.energo.phoenix.model.enums.template.ContractTemplateFileName;
import bg.energo.phoenix.model.enums.template.ContractTemplateSigning;
import bg.energo.phoenix.repository.billing.companyDetails.CompanyDetailRepository;
import bg.energo.phoenix.repository.billing.companyDetails.CompanyLogoRepository;
import bg.energo.phoenix.repository.documents.DocumentsRepository;
import bg.energo.phoenix.repository.nomenclature.product.GridOperatorRepository;
import bg.energo.phoenix.repository.receivable.disconnectionPowerSupplyRequest.DisconnectionOfPowerRequestTemplatesRepository;
import bg.energo.phoenix.repository.receivable.disconnectionPowerSupplyRequest.DisconnectionPowerSupplyRequestRepository;
import bg.energo.phoenix.repository.receivable.disconnectionPowerSupplyRequest.DisconnectionPowerSupplyRequestsDocumentsRepository;
import bg.energo.phoenix.repository.receivable.disconnectionPowerSupplyRequest.DisconnectionPowerSupplyRequestsResultsRepository;
import bg.energo.phoenix.repository.template.ContractTemplateDetailsRepository;
import bg.energo.phoenix.repository.template.ContractTemplateRepository;
import bg.energo.phoenix.service.document.enums.DocumentPathPayloads;
import bg.energo.phoenix.service.document.enums.FileFormat;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.service.signing.SignerChainManager;
import bg.energo.phoenix.util.template.document.DocumentGenerationUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static bg.energo.phoenix.exception.ErrorCode.APPLICATION_ERROR;

@Slf4j
@Service
public class DisconnectionPowerSupplyRequestsDocumentCreationService extends AbstractDocumentCreationService {

    public static final String REQUEST_FOR_DISCONNECTION_LABEL = "Request_for_Disconnection";
    private final DisconnectionPowerSupplyRequestRepository disconnectionPowerSupplyRequestRepository;
    private final DisconnectionOfPowerRequestTemplatesRepository disconnectionOfPowerRequestTemplatesRepository;
    private final DisconnectionPowerSupplyRequestsDocumentsRepository disconnectionPowerSupplyRequestsDocumentsRepository;
    private final GridOperatorRepository gridOperatorRepository;
    private final DisconnectionPowerSupplyRequestsResultsRepository disconnectionPowerSupplyRequestsResultsRepository;
    private static final String FOLDER_PATH = "disconnection_document";

    public DisconnectionPowerSupplyRequestsDocumentCreationService(
            ContractTemplateDetailsRepository contractTemplateDetailsRepository,
            ContractTemplateRepository contractTemplateRepository,
            DocumentGenerationService documentGenerationService,
            DocumentGenerationUtil documentGenerationUtil,
            DocumentsRepository documentsRepository,
            CompanyDetailRepository companyDetailRepository,
            CompanyLogoRepository companyLogoRepository,
            SignerChainManager signerChainManager,
            FileService fileService,
            DisconnectionPowerSupplyRequestRepository disconnectionPowerSupplyRequestRepository,
            DisconnectionOfPowerRequestTemplatesRepository disconnectionOfPowerRequestTemplatesRepository,
            DisconnectionPowerSupplyRequestsDocumentsRepository disconnectionPowerSupplyRequestsDocumentsRepository,
            GridOperatorRepository gridOperatorRepository,
            DisconnectionPowerSupplyRequestsResultsRepository disconnectionPowerSupplyRequestsResultsRepository) {
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
        this.disconnectionPowerSupplyRequestRepository = disconnectionPowerSupplyRequestRepository;
        this.disconnectionOfPowerRequestTemplatesRepository = disconnectionOfPowerRequestTemplatesRepository;
        this.disconnectionPowerSupplyRequestsDocumentsRepository = disconnectionPowerSupplyRequestsDocumentsRepository;
        this.gridOperatorRepository = gridOperatorRepository;
        this.disconnectionPowerSupplyRequestsResultsRepository = disconnectionPowerSupplyRequestsResultsRepository;
    }

    /**
     * Generates documents for a given disconnection power supply request.
     *
     * This method retrieves the disconnection power supply request by the provided ID, and then generates the necessary documents
     * based on the templates associated with the request. The generated documents are then signed using the signerChainManager.
     *
     * @param disconnectionRequestId the ID of the disconnection power supply request
     */
    @Transactional
    public void generateDocuments(Long disconnectionRequestId) {
        DisconnectionPowerSupplyRequests request = disconnectionPowerSupplyRequestRepository
                .findById(disconnectionRequestId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Disconnection power supply request not found by ID: %s".formatted(disconnectionRequestId)));

        List<DisconnectionOfPowerSupplyRequestTemplate> templates = disconnectionOfPowerRequestTemplatesRepository.findByDisconnectionId(disconnectionRequestId);
        if (templates.isEmpty()) {
            log.info("No templates found for disconnection power supply request ID: {}", disconnectionRequestId);
            return;
        }

        DisconnectionPowerSupplyRequestsDocumentModel documentModel = buildModelWithSharedInfo(new DisconnectionPowerSupplyRequestsDocumentModel());
        mapDisconnectionJsonTemplate(request, documentModel);

        List<Document> documentList = new ArrayList<>();

        for (DisconnectionOfPowerSupplyRequestTemplate template : templates) {
            try {
                DocumentGenerationResult result = generateDocument(template, documentModel, buildFileDestinationPath(), request);
                if (result.document() != null) {
                    documentList.add(result.document());
                }
            } catch (Exception e) {
                log.error("Failed to generate document for template {}: {}", template.getTemplateId(), e.getMessage());
                throw new ClientException("Failed to generate document: %s".formatted(e.getMessage()), APPLICATION_ERROR);
            }
        }

        if (!documentList.isEmpty()) {
            signerChainManager.startSign(documentList);
        }
    }

    /**
     * Maps the disconnection power supply request data to the document model.
     *
     * This method retrieves the grid operator information for the given request, and then fills the document model
     * with the necessary data, including the request number, creation date, grid operator name and full name,
     * supplier type, and the list of POD (Point of Delivery) information.
     *
     * @param request the disconnection power supply request
     * @param documentModel the document model to be filled
     */
    private void mapDisconnectionJsonTemplate(DisconnectionPowerSupplyRequests request, DisconnectionPowerSupplyRequestsDocumentModel documentModel) {
        GridOperator gridOperator = gridOperatorRepository
                .findById(request.getGridOperatorId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Can't find grid operator with id: %s".formatted(request.getGridOperatorId())));

        List<DisconnectionPowerSupplyRequestsDocumentPodImpl> podImpl = getPodInfo(request.getId());
        documentModel.fillDocumentData(
                request.getRequestNumber(),
                request.getCreateDate(),
                gridOperator.getName(),
                gridOperator.getFullName(),
                request.getSupplierType().name(),
                podImpl
        );
    }

    /**
     * Generates a document for a disconnection power supply request using the provided template, model, and request information.
     *
     * This method retrieves the necessary contract template and template details, formats the document file name, and generates the document using the provided template file, destination path, and model data. It then saves the generated document to the database and returns a `DocumentGenerationResult` containing the generated document paths, template details, and the resulting PDF document.
     *
     * @param template the disconnection power supply request template to use for generating the document
     * @param model the document model containing the data to be used in the document generation
     * @param destinationPath the path where the generated document should be saved
     * @param request the disconnection power supply request for which the document is being generated
     * @return a `DocumentGenerationResult` containing the generated document paths, template details, and the resulting PDF document
     * @throws Exception if there is an error during the document generation or saving process
     */
    private DocumentGenerationResult generateDocument(
            DisconnectionOfPowerSupplyRequestTemplate template,
            DisconnectionPowerSupplyRequestsDocumentModel model,
            String destinationPath,
            DisconnectionPowerSupplyRequests request
    ) throws Exception {
        ContractTemplateDetail templateDetail = getContractTemplateLastDetails(template.getTemplateId());
        String fileName = formatDocumentFileName(request, templateDetail);
        ByteArrayResource templateFileResource = createTemplateFileResource(templateDetail);

        Set<FileFormat> fileFormats = templateDetail
                .getOutputFileFormat()
                .stream()
                .map(format -> FileFormat.valueOf(format.name()))
                .collect(Collectors.toSet());

        log.debug("Generating document for template: {}", template.getTemplateId());
        DocumentPathPayloads documentPathPayloads = documentGenerationService.generateDocument(
                templateFileResource,
                destinationPath,
                fileName,
                model,
                fileFormats,
                false
        );
        log.debug("Document was created successfully");

        Document resultDocument = null;
        for (ContractTemplateFileFormat format : templateDetail.getOutputFileFormat()) {
            String generatedDocumentPath = documentGenerationUtil.getGeneratedDocumentPath(FileFormat.valueOf(format.name()), documentPathPayloads);
            Document savedDocument = saveDocuments(
                    String.format("%s.%s", fileName, format.name().toLowerCase()),
                    generatedDocumentPath,
                    FileFormat.valueOf(format.name()),
                    getDocumentSigners(template, templateDetail),
                    request.getId(),
                    template.getTemplateId()
            );
            if (ContractTemplateFileFormat.PDF.equals(format)) {
                resultDocument = savedDocument;
            }
        }

        return new DocumentGenerationResult(documentPathPayloads, templateDetail, resultDocument);
    }

    /**
     * Retrieves the list of document signers for the given contract template details.
     *
     * @param template The disconnection of power supply request template.
     * @param templateDetail The contract template details.
     * @return The list of document signers, or an empty list if no signers are defined in the template details.
     */
    private List<DocumentSigners> getDocumentSigners(DisconnectionOfPowerSupplyRequestTemplate template, ContractTemplateDetail templateDetail) {
        return Optional
                .ofNullable(templateDetail.getFileSigning())
                .orElse(Collections.emptyList())
                .stream()
                .map(ContractTemplateSigning::getDocumentSigners)
                .toList();
    }

    /**
     * Saves a document related to a disconnection power supply request.
     *
     * @param fileName The name of the document file.
     * @param ftpPath The FTP path where the document is stored.
     * @param fileFormat The format of the document file.
     * @param documentSigners The list of document signers.
     * @param disconnectionRequestId The ID of the disconnection power supply request.
     * @param templateId The ID of the contract template used to generate the document.
     * @return The saved document, or null if the FTP path is null.
     */
    private Document saveDocuments(
            String fileName,
            String ftpPath,
            FileFormat fileFormat,
            List<DocumentSigners> documentSigners,
            Long disconnectionRequestId,
            Long templateId
    ) {

        if (ftpPath == null) {
            return null;
        }

        Document document = Document.builder()
                .signers(documentSigners)
                .signedBy(new ArrayList<>())
                .name(fileName)
                .unsignedFileUrl(ftpPath)
                .signedFileUrl(ftpPath)
                .fileFormat(fileFormat)
                .templateId(templateId)
                .documentStatus(DocumentStatus.UNSIGNED)
                .status(EntityStatus.ACTIVE)
                .build();

        Document savedDoc = documentsRepository.saveAndFlush(document);

        disconnectionPowerSupplyRequestsDocumentsRepository.save(
                new DisconnectionPowerSupplyRequestsDocuments(
                        null,
                        savedDoc.getId(),
                        disconnectionRequestId,
                        EntityStatus.ACTIVE
                )
        );

        return savedDoc;
    }

    /**
     * Formats the document file name for a disconnection power supply request based on the provided contract template details.
     *
     * @param requests The disconnection power supply request object.
     * @param contractTemplateDetail The contract template details used to format the file name.
     * @return The formatted file name.
     */
    private String formatDocumentFileName(DisconnectionPowerSupplyRequests requests, ContractTemplateDetail contractTemplateDetail) {
        log.debug("Starting file name formatting");
        List<String> fileNameParts = new ArrayList<>();
        log.debug("Extracting file prefix");
        fileNameParts.add(documentGenerationUtil.extractFilePrefix(contractTemplateDetail));
        log.debug("Extracting file name");
        fileNameParts.add(extractFileName(requests, contractTemplateDetail));
        log.debug("Extracting file suffix");
        fileNameParts.add(extractFileSuffix(requests.getCreateDate(), contractTemplateDetail));
        fileNameParts.removeIf(StringUtils::isBlank);

        String fileParts = String.join("_", fileNameParts);
        fileParts = StringUtils.isBlank(fileParts) ? "POWER_SUPPLY_DISCONNECTION_REQUESTS" : fileParts;
        return fileParts.substring(0, Math.min(fileParts.length(), 200)).replaceAll("/", "_");
    }

    /**
     * Extracts the file name for a disconnection power supply request document based on the provided contract template details.
     *
     * @param requests The disconnection power supply request object.
     * @param contractTemplateDetail The contract template details used to format the file name.
     * @return The formatted file name.
     */
    private String extractFileName(DisconnectionPowerSupplyRequests requests, ContractTemplateDetail contractTemplateDetail) {
        try {
            List<ContractTemplateFileName> fileName = contractTemplateDetail
                    .getFileName()
                    .stream()
                    .sorted(Comparator.comparing(ContractTemplateFileName::getPriority))
                    .toList();

            if (CollectionUtils.isEmpty(fileName)) {
                return requests.getRequestNumber();
            } else {
                List<String> nameParts = new ArrayList<>();
                nameParts.add(REQUEST_FOR_DISCONNECTION_LABEL);
                for (ContractTemplateFileName contractTemplateFileName : fileName) {
                    switch (contractTemplateFileName) {
                        case CUSTOMER_IDENTIFIER, CUSTOMER_NAME, CUSTOMER_NUMBER -> {
                            //these fields are disabled
                        }
                        case DOCUMENT_NUMBER -> nameParts.add(requests.getRequestNumber());
                        case FILE_ID ->
                                nameParts.add(String.valueOf(disconnectionPowerSupplyRequestsDocumentsRepository.getNextIdValue()));
                        case TIMESTAMP -> nameParts.add(requests.getCreateDate().toString());
                    }
                }

                return String.join("_", nameParts);
            }
        } catch (Exception e) {
            log.error("Exception handled while trying to extract file name", e);
            return requests.getRequestNumber();
        }
    }

    /**
     * Retrieves a list of {@link DisconnectionPowerSupplyRequestsDocumentPodImpl} objects for the given request ID.
     * The method fetches the POD (Point of Delivery) information from the
     * {@link DisconnectionPowerSupplyRequestsResultsRepository} and maps it to the
     * {@link DisconnectionPowerSupplyRequestsDocumentPodImpl} objects.
     *
     * @param requestId The ID of the disconnection power supply request.
     * @return A list of {@link DisconnectionPowerSupplyRequestsDocumentPodImpl} objects containing the POD information.
     */
    private List<DisconnectionPowerSupplyRequestsDocumentPodImpl> getPodInfo(Long requestId) {
        return disconnectionPowerSupplyRequestsResultsRepository
                .getPodImplForDocument(requestId)
                .stream()
                .map(response -> {
                            var pod = new DisconnectionPowerSupplyRequestsDocumentPodImpl();
                            pod.fillPodsInfo(response);
                            return pod;
                        }
                )
                .toList();
    }

    @Override
    protected String folderPath() {
        return FOLDER_PATH;
    }

    /**
     * Represents the result of a document generation operation, including the generated document paths, the contract template details, and the generated document.
     */
    private record DocumentGenerationResult(
            DocumentPathPayloads documentPaths,
            ContractTemplateDetail templateDetail,
            Document document
    ) {
    }
}
