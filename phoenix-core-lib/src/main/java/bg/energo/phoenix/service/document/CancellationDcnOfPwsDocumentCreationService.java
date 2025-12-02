package bg.energo.phoenix.service.document;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.entity.nomenclature.product.GridOperator;
import bg.energo.phoenix.model.entity.receivable.cancellationOfDisconnectionOfThePowerSupply.CancellationDcnDocFile;
import bg.energo.phoenix.model.entity.receivable.cancellationOfDisconnectionOfThePowerSupply.CancellationOfDisconnectionOfThePowerSupply;
import bg.energo.phoenix.model.entity.receivable.disconnectionPowerSupplyRequests.DisconnectionPowerSupplyRequests;
import bg.energo.phoenix.model.entity.template.ContractTemplate;
import bg.energo.phoenix.model.entity.template.ContractTemplateDetail;
import bg.energo.phoenix.model.enums.documents.DocumentSigners;
import bg.energo.phoenix.model.enums.template.ContractTemplateFileFormat;
import bg.energo.phoenix.model.enums.template.ContractTemplateFileName;
import bg.energo.phoenix.model.response.receivable.cancellationOfDisconnectionOfThePowerSupply.impl.CancellationDcnOfPwsDocumentImpl;
import bg.energo.phoenix.model.response.receivable.cancellationOfDisconnectionOfThePowerSupply.impl.CancellationDcnOfPwsDocumentPodImpl;
import bg.energo.phoenix.repository.billing.companyDetails.CompanyDetailRepository;
import bg.energo.phoenix.repository.billing.companyDetails.CompanyLogoRepository;
import bg.energo.phoenix.repository.documents.DocumentsRepository;
import bg.energo.phoenix.repository.nomenclature.product.GridOperatorRepository;
import bg.energo.phoenix.repository.receivable.cancellationOfDisconnectionOfThePowerSupply.CancellationDcnDocFileRepository;
import bg.energo.phoenix.repository.receivable.cancellationOfDisconnectionOfThePowerSupply.CancellationOfDisconnectionOfThePowerSupplyRepository;
import bg.energo.phoenix.repository.receivable.disconnectionPowerSupplyRequest.DisconnectionPowerSupplyRequestRepository;
import bg.energo.phoenix.repository.template.ContractTemplateDetailsRepository;
import bg.energo.phoenix.repository.template.ContractTemplateRepository;
import bg.energo.phoenix.service.document.enums.DocumentPathPayloads;
import bg.energo.phoenix.service.document.enums.FileFormat;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.service.signing.SignerChainManager;
import bg.energo.phoenix.util.template.document.DocumentGenerationUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static bg.energo.phoenix.exception.ErrorCode.APPLICATION_ERROR;

@Slf4j
@Service
public class CancellationDcnOfPwsDocumentCreationService extends AbstractDocumentCreationService {
    private static final String FOLDER_PATH = "cancellation_of_request_for_dcn";
    public static final String CANCELLATION_OF_REQUEST_FOR_DISCONNECTION_LABEL = "Cancellation_of_Request_for_Disconnection";
    private final GridOperatorRepository gridOperatorRepository;
    private final CancellationOfDisconnectionOfThePowerSupplyRepository cancellationRepository;
    private final DisconnectionPowerSupplyRequestRepository disconnectionPowerSupplyRequestRepository;
    private final CancellationDcnDocFileRepository cancellationDcnDocFileRepository;

    public CancellationDcnOfPwsDocumentCreationService(
            ContractTemplateDetailsRepository contractTemplateDetailsRepository,
            ContractTemplateRepository contractTemplateRepository,
            DocumentGenerationService documentGenerationService,
            DocumentGenerationUtil documentGenerationUtil,
            DocumentsRepository documentsRepository,
            CompanyDetailRepository companyDetailRepository,
            CompanyLogoRepository companyLogoRepository,
            SignerChainManager signerChainManager,
            FileService fileService,
            GridOperatorRepository gridOperatorRepository,
            CancellationOfDisconnectionOfThePowerSupplyRepository cancellationRepository,
            DisconnectionPowerSupplyRequestRepository disconnectionPowerSupplyRequestRepository,
            CancellationDcnDocFileRepository cancellationDcnDocFileRepository
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
        this.gridOperatorRepository = gridOperatorRepository;
        this.cancellationRepository = cancellationRepository;
        this.disconnectionPowerSupplyRequestRepository = disconnectionPowerSupplyRequestRepository;
        this.cancellationDcnDocFileRepository = cancellationDcnDocFileRepository;
    }


    /**
     * Generates documents and triggers signing process for a cancellation request
     *
     * @param cancellationId ID of the cancellation request
     * @return List of generated document IDs
     * @throws ClientException if document generation fails
     * @throws DomainEntityNotFoundException if required entities are not found
     */
    @Transactional
    public List<Long> generateDocument(Long cancellationId) {
        CancellationDcnOfPwsDocumentImpl model = buildModelWithSharedInfo(new CancellationDcnOfPwsDocumentImpl());
        List<Document> documentsToSign = new ArrayList<>();
        List<Long> documentIds = new ArrayList<>();

        CancellationOfDisconnectionOfThePowerSupply cancellation = cancellationRepository
                .findById(cancellationId)
                .orElseThrow(
                        () -> new DomainEntityNotFoundException("Cancellation of request for disconnection not found with id: %s".formatted(cancellationId))
                );

        DisconnectionPowerSupplyRequests disconnectionPowerSupplyRequests = disconnectionPowerSupplyRequestRepository
                .findById(cancellation.getRequestForDisconnectionOfThePowerSupplyId())
                .orElseThrow(
                        () -> new DomainEntityNotFoundException("Request for disconnection not found with id: %s: ".formatted(cancellation.getRequestForDisconnectionOfThePowerSupplyId()))
                );

        GridOperator gridOperator = gridOperatorRepository
                .findById(disconnectionPowerSupplyRequests.getGridOperatorId())
                .orElseThrow(
                        () -> new DomainEntityNotFoundException("GridOperator not found with id: %s;".formatted(disconnectionPowerSupplyRequests.getGridOperatorId()))
                );

        mapDataIntoModel(cancellation, model, gridOperator, disconnectionPowerSupplyRequests);

        List<ContractTemplate> templateList = cancellationRepository.getTemplatesIfExists(cancellationId);

        if (!templateList.isEmpty()) {
            for (ContractTemplate temp : templateList) {
                ContractTemplateDetail templateDetail = getContractTemplateDetails(temp.getLastTemplateDetailId());
                List<DocumentSigners> documentSigners = getDocumentSigners(templateDetail);
                try {
                    ByteArrayResource emailTemplateFileResource = createTemplateFileResource(templateDetail);

                    for (ContractTemplateFileFormat format : templateDetail.getOutputFileFormat()) {
                        FileFormat fileFormat = FileFormat.fromContractTemplateFileFormat(format);
                        String fileName = formatDocumentFileName(cancellation, templateDetail, fileFormat, gridOperator);

                        log.debug("Generating document for format: {}", fileFormat);
                        DocumentPathPayloads documentPathPayloads = documentGenerationService.generateDocument(
                                emailTemplateFileResource,
                                buildFileDestinationPath(),
                                fileName,
                                model,
                                Set.of(fileFormat),
                                false
                        );

                        String fileUrl = switch (fileFormat) {
                            case PDF -> documentPathPayloads.pdfPath();
                            case DOCX -> documentPathPayloads.docXPath();
                            case XLSX -> documentPathPayloads.xlsxPath();
                        };

                        Document document = saveDocument(
                                fileName,
                                fileUrl,
                                fileFormat,
                                documentSigners,
                                templateDetail.getTemplateId()
                        );

                        CancellationDcnDocFile documentFile = cancellationDcnDocFileRepository.save(
                                CancellationDcnDocFile.builder()
                                        .documentId(document.getId())
                                        .cancellationId(cancellationId)
                                        .status(EntityStatus.ACTIVE)
                                        .build()
                        );
                        documentIds.add(documentFile.getId());

                        if (FileFormat.PDF.equals(fileFormat)) {
                            documentsToSign.add(document);
                        }

                    }
                } catch (Exception e) {
                    log.error("Exception while storing template files: {}", e.getMessage());
                    throw new ClientException("Exception while storing template files: %s".formatted(e.getMessage()), APPLICATION_ERROR);
                }

            }
            if (!documentsToSign.isEmpty()) {
                signerChainManager.startSign(documentsToSign);
            }
        }
        return documentIds;
    }

    /**
     * Maps data from entities into document model
     *
     * @param cancellation Cancellation request data
     * @param model Document model to populate
     * @param gridOperator Grid operator details
     * @param disconnectionRequest Original disconnection request
     */
    private void mapDataIntoModel(
            CancellationOfDisconnectionOfThePowerSupply cancellation,
            CancellationDcnOfPwsDocumentImpl model,
            GridOperator gridOperator,
            DisconnectionPowerSupplyRequests disconnectionRequest
    ) {
        String creationDate = formatCreationDate(cancellation.getCreateDate());
        List<CancellationDcnOfPwsDocumentPodImpl> pods = getPodInfo(cancellation.getId());

        model.fillCancellationData(
                cancellation.getNumber(),
                creationDate,
                gridOperator.getName(),
                gridOperator.getFullName(),
                disconnectionRequest.getSupplierType().toString(),
                pods
        );
    }

    private String formatCreationDate(LocalDateTime date) {
        return date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }

    /**
     * Gets points of delivery details for document
     *
     * @param cancellationId ID of cancellation request
     * @return List of POD details for document
     */
    private List<CancellationDcnOfPwsDocumentPodImpl> getPodInfo(Long cancellationId) {
        return cancellationRepository
                .getDocumentInfo(cancellationId)
                .stream()
                .map(response -> {
                            var pod = new CancellationDcnOfPwsDocumentPodImpl();
                            pod.fillPodsInfo(response);
                            return pod;
                        }
                )
                .toList();
    }

    public String formatDocumentFileName(CancellationOfDisconnectionOfThePowerSupply cancellation, ContractTemplateDetail contractTemplateDetail, FileFormat fileFormat, GridOperator gridOperator) {
        log.debug("Starting file name formatting");
        List<String> fileNameParts = new ArrayList<>();
        log.debug("Extracting file prefix");
        fileNameParts.add(documentGenerationUtil.extractFilePrefix(contractTemplateDetail));
        log.debug("Extracting file name");
        fileNameParts.add(extractFileName(cancellation, contractTemplateDetail, gridOperator));
        log.debug("Extracting file suffix");
        fileNameParts.add(extractFileSuffix(cancellation.getCreateDate(), contractTemplateDetail));
        fileNameParts.removeIf(StringUtils::isBlank);
        String fileParts = String.join("_", fileNameParts);
        fileParts = StringUtils.isBlank(fileParts) ? "ObjectionWithdrawal" : fileParts;
        return fileParts.substring(0, Math.min(fileParts.length(), 200)).concat(".").concat(fileFormat.getSuffix()).replaceAll("/", "_");
    }

    /**
     * Extracts dynamic filename based on template configuration
     *
     * @param cancellation Cancellation request entity
     * @param contractTemplateDetail Template details with filename config
     * @param gridOperator Grid operator details
     * @return Generated filename
     */

    private String extractFileName(CancellationOfDisconnectionOfThePowerSupply cancellation, ContractTemplateDetail contractTemplateDetail, GridOperator gridOperator) {
        try {
            List<ContractTemplateFileName> fileName = contractTemplateDetail
                    .getFileName()
                    .stream()
                    .sorted(Comparator.comparing(ContractTemplateFileName::getPriority))
                    .toList();

            if (org.springframework.util.CollectionUtils.isEmpty(fileName)) {
                return cancellation.getNumber();
            } else {
                List<String> nameParts = new ArrayList<>();
                nameParts.add(CANCELLATION_OF_REQUEST_FOR_DISCONNECTION_LABEL);
                for (ContractTemplateFileName contractTemplateFileName : fileName) {
                    switch (contractTemplateFileName) {
                        case CUSTOMER_IDENTIFIER -> //TODO waiting for template change
                                nameParts.add("customerIdentifierTest");
                        case CUSTOMER_NAME -> //TODO waiting for template change
                                nameParts.add(gridOperator.getFullName());
                        case CUSTOMER_NUMBER -> //TODO waiting for template change
                                nameParts.add("customerNumberTest");
                        case DOCUMENT_NUMBER -> nameParts.add(cancellation.getNumber());
                        case FILE_ID ->
                                nameParts.add(String.valueOf(cancellationDcnDocFileRepository.getNextIdValue()));
                        case TIMESTAMP -> nameParts.add(cancellation.getCreateDate().toString());
                    }
                }

                return String.join("_", nameParts);
            }
        } catch (Exception e) {
            log.error("Exception handled while trying to extract file name", e);
            return cancellation.getNumber();
        }
    }

    @Override
    protected String folderPath() {
        return FOLDER_PATH;
    }
}
