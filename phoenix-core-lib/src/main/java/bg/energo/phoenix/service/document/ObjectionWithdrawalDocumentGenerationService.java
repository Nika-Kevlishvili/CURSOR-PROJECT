package bg.energo.phoenix.service.document;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.entity.nomenclature.product.GridOperator;
import bg.energo.phoenix.model.entity.receivable.balancingGroupCoordinatorObjection.ObjectionToChangeOfCbg;
import bg.energo.phoenix.model.entity.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator.ObjectionWithdrawalToAChangeOfABalancingGroupCoordinator;
import bg.energo.phoenix.model.entity.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator.ObjectionWithdrawalToCbgDocFile;
import bg.energo.phoenix.model.entity.template.ContractTemplate;
import bg.energo.phoenix.model.entity.template.ContractTemplateDetail;
import bg.energo.phoenix.model.enums.documents.DocumentSigners;
import bg.energo.phoenix.model.enums.receivable.balancingGroupCoordinatorObjection.DocumentGenerationType;
import bg.energo.phoenix.model.enums.template.ContractTemplateFileFormat;
import bg.energo.phoenix.model.enums.template.ContractTemplateFileName;
import bg.energo.phoenix.model.response.receivable.balancingGroupCoordinatorObjection.ObjectionDocumentGenerationResponse;
import bg.energo.phoenix.model.response.receivable.balancingGroupCoordinatorObjection.impl.ObjectionOfCbgDocumentImpl;
import bg.energo.phoenix.model.response.receivable.balancingGroupCoordinatorObjection.impl.ObjectionOfCbgDocumentPodImpl;
import bg.energo.phoenix.repository.billing.companyDetails.CompanyDetailRepository;
import bg.energo.phoenix.repository.billing.companyDetails.CompanyLogoRepository;
import bg.energo.phoenix.repository.documents.DocumentsRepository;
import bg.energo.phoenix.repository.nomenclature.product.GridOperatorRepository;
import bg.energo.phoenix.repository.receivable.balancingGroupCoordinatorObjection.ObjectionToChangeOfCbgRepository;
import bg.energo.phoenix.repository.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator.ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorRepository;
import bg.energo.phoenix.repository.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator.ObjectionWithdrawalToAChangeOfABalancingGroupProcessResultRepository;
import bg.energo.phoenix.repository.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator.ObjectionWithdrawalToCbgDocFileRepository;
import bg.energo.phoenix.repository.template.ContractTemplateDetailsRepository;
import bg.energo.phoenix.repository.template.ContractTemplateRepository;
import bg.energo.phoenix.service.document.enums.DocumentPathPayloads;
import bg.energo.phoenix.service.document.enums.FileFormat;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.service.signing.SignerChainManager;
import bg.energo.phoenix.util.template.document.DocumentGenerationUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;

import static bg.energo.phoenix.exception.ErrorCode.APPLICATION_ERROR;

@Slf4j
@Service
public class ObjectionWithdrawalDocumentGenerationService extends AbstractDocumentCreationService {

    public static final String OBJECTION_WITHDRAWAL_LABEL = "Objection_Withdrawal";
    private final ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorRepository withdrawalRepository;
    private final GridOperatorRepository gridOperatorRepository;
    private final ObjectionWithdrawalToCbgDocFileRepository objectionWithdrawalToCbgDocFileRepository;
    private final ObjectionWithdrawalToAChangeOfABalancingGroupProcessResultRepository processResultRepository;
    private final ObjectionToChangeOfCbgRepository objectionToChangeOfCbgRepository;
    private static final String FOLDER_PATH = "objection_to_change_of_cbg_document";

    public ObjectionWithdrawalDocumentGenerationService(
            ContractTemplateDetailsRepository contractTemplateDetailsRepository,
            ContractTemplateRepository contractTemplateRepository,
            DocumentGenerationService documentGenerationService,
            DocumentGenerationUtil documentGenerationUtil,
            DocumentsRepository documentsRepository,
            CompanyDetailRepository companyDetailRepository,
            CompanyLogoRepository companyLogoRepository,
            SignerChainManager signerChainManager,
            FileService fileService,
            ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorRepository withdrawalRepository,
            GridOperatorRepository gridOperatorRepository,
            ObjectionWithdrawalToCbgDocFileRepository objectionWithdrawalToCbgDocFileRepository,
            ObjectionWithdrawalToAChangeOfABalancingGroupProcessResultRepository processResultRepository,
            ObjectionToChangeOfCbgRepository objectionToChangeOfCbgRepository
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
        this.withdrawalRepository = withdrawalRepository;
        this.gridOperatorRepository = gridOperatorRepository;
        this.objectionWithdrawalToCbgDocFileRepository = objectionWithdrawalToCbgDocFileRepository;
        this.processResultRepository = processResultRepository;
        this.objectionToChangeOfCbgRepository = objectionToChangeOfCbgRepository;
    }

    /**
     * Generates documents for an objection withdrawal based on the specified type.
     * Generates either email templates or document templates with proper formatting and signing chain.
     *
     * @param withdrawalId The ID of the withdrawal to generate documents for
     * @param type         The type of document to generate (EMAIL_TEMPLATE or DOCUMENT_TEMPLATE)
     * @return ObjectionDocumentGenerationResponse containing generated document IDs and metadata
     * @throws DomainEntityNotFoundException if withdrawal, objection, grid operator, or templates are not found
     * @throws ClientException               if there's an error during document generation or storage
     */
    @Transactional
    public ObjectionDocumentGenerationResponse generateDocument(Long withdrawalId, DocumentGenerationType type) {
        List<Long> documentIds = new ArrayList<>();
        List<Document> documentsToSign = new ArrayList<>();
        List<ObjectionWithdrawalToCbgDocFile> documentFiles = new ArrayList<>();
        ObjectionDocumentGenerationResponse documentResponse = new ObjectionDocumentGenerationResponse();

        ObjectionOfCbgDocumentImpl model = buildModelWithSharedInfo(new ObjectionOfCbgDocumentImpl());

        ObjectionWithdrawalToAChangeOfABalancingGroupCoordinator withdrawal = withdrawalRepository
                .findById(withdrawalId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Objection withdrawal not found wih id: %s".formatted(withdrawalId)));

        ObjectionToChangeOfCbg objection = objectionToChangeOfCbgRepository
                .findById(withdrawal.getChangeOfCbgId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Can't find Objection with id: %s ".formatted(withdrawal.getChangeOfCbgId())));

        GridOperator gridOperator = gridOperatorRepository
                .findById(objection.getGridOperatorId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Can't find grid operator with id: %s".formatted(objection.getGridOperatorId())));

        mapDataIntoModel(withdrawal, objection, model, gridOperator);
        if (type.equals(DocumentGenerationType.EMAIL_TEMPLATE)) {
            ContractTemplateDetail templateDetail = getContractTemplateLastDetails(withdrawal.getEmailTemplateId());
            String fileName = formatDocumentFileName(withdrawal, templateDetail, FileFormat.DOCX, gridOperator);
            try {
                ByteArrayResource emailTemplateFileResource = createTemplateFileResource(templateDetail);

                log.debug("Generating document for format: {}", FileFormat.DOCX);
                DocumentPathPayloads documentPathPayloads = documentGenerationService.generateDocument(
                        emailTemplateFileResource,
                        buildFileDestinationPath(),
                        fileName,
                        model,
                        Set.of(FileFormat.DOCX),
                        false
                );

                ObjectionWithdrawalToCbgDocFile document = objectionWithdrawalToCbgDocFileRepository.save(
                        ObjectionWithdrawalToCbgDocFile.builder()
                                .fileUrl(documentPathPayloads.docXPath())
                                .templateId(templateDetail.getTemplateId())
                                .fileName(fileName)
                                .status(EntityStatus.ACTIVE)
                                .changeOfWithdrawalId(withdrawal.getId())
                                .build()
                );

                documentResponse.setEmailDocumentId(Collections.singletonMap(document.getId(), templateDetail.getSubject()));
            } catch (Exception e) {
                log.error("Exception while storing template files: {}", e.getMessage());
                throw new ClientException("Exception while storing template files: %s".formatted(e.getMessage()), APPLICATION_ERROR);
            }
        } else {
            List<ContractTemplate> documentTemplateList = withdrawalRepository.findRelatedContractTemplates(withdrawal.getId());

            for (ContractTemplate template : documentTemplateList) {
                ContractTemplateDetail templateDetail = getContractTemplateDetails(template.getLastTemplateDetailId());
                List<DocumentSigners> documentSigners = getDocumentSigners(templateDetail);
                try {
                    ByteArrayResource documentTemplateFileResource = createTemplateFileResource(templateDetail);
                    log.debug("Clone of template file created");
                    for (ContractTemplateFileFormat format : templateDetail.getOutputFileFormat()) {
                        FileFormat fileFormat = FileFormat.fromContractTemplateFileFormat(format);
                        String fileName = formatDocumentFileName(withdrawal, templateDetail, fileFormat, gridOperator);

                        log.debug("Generating document for format: {}", fileFormat);
                        DocumentPathPayloads documentPathPayloads = documentGenerationService.generateDocument(
                                documentTemplateFileResource,
                                buildFileDestinationPath(),
                                fileName,
                                model,
                                Set.of(fileFormat),
                                false
                        );
                        log.debug("Document was created successfully for format: {}", fileFormat);

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

                        ObjectionWithdrawalToCbgDocFile documentFile = objectionWithdrawalToCbgDocFileRepository.save(
                                ObjectionWithdrawalToCbgDocFile.builder()
                                        .fileUrl(fileUrl)
                                        .templateId(templateDetail.getTemplateId())
                                        .fileName(fileName)
                                        .status(EntityStatus.ACTIVE)
                                        .changeOfWithdrawalId(withdrawal.getId())
                                        .documentId(document.getId())
                                        .build()
                        );
                        documentIds.add(documentFile.getId());

                        if (document != null && FileFormat.PDF.equals(fileFormat)) {
                            documentsToSign.add(document);
                            documentFiles.add(documentFile);
                        }
                    }
                } catch (Exception e) {
                    log.error("Exception while storing template files: {}", e.getMessage());
                    throw new ClientException("Exception while storing template files: %s".formatted(e.getMessage()), APPLICATION_ERROR);
                }
            }
        }

        if (!documentsToSign.isEmpty()) {
            signerChainManager.startSign(documentsToSign);
        }
        for (int i = 0; i < documentsToSign.size(); i++) {
            Document doc = documentsToSign.get(i);
            ObjectionWithdrawalToCbgDocFile docFile = documentFiles.get(i);
            docFile.setFileUrl(doc.getSignedFileUrl());
        }
        objectionWithdrawalToCbgDocFileRepository.saveAll(documentFiles);
        documentResponse.setDocumentIds(documentIds);
        return documentResponse;
    }

    /**
     * Maps withdrawal and objection data into the document model for generation.
     *
     * @param objectionWithdrawal The withdrawal entity containing base information
     * @param objection           The related objection entity
     * @param model               The document model to populate
     * @param gridOperator        The associated grid operator entity
     */
    private void mapDataIntoModel(ObjectionWithdrawalToAChangeOfABalancingGroupCoordinator objectionWithdrawal, ObjectionToChangeOfCbg objection, ObjectionOfCbgDocumentImpl model, GridOperator gridOperator) {
        List<ObjectionOfCbgDocumentPodImpl> podImpl = processResultRepository.getPodImpl(objectionWithdrawal.getId());

        model.fillObjectionData(
                objectionWithdrawal.getWithdrawalChangeOfCbgNumber(),
                gridOperator.getName(),
                objectionWithdrawal.getCreateDate(),
                objection.getChangeDate(),
                podImpl
        );
    }

    /**
     * Formats the document filename based on template details and entity information.
     * Combines prefix, filename parts, and suffix according to template configuration.
     *
     * @param objectionWithdrawal    The withdrawal entity containing base information
     * @param contractTemplateDetail The template details containing filename configuration
     * @param fileFormat             The output file format
     * @param gridOperator           The associated grid operator entity
     * @return Formatted filename string
     */
    public String formatDocumentFileName(ObjectionWithdrawalToAChangeOfABalancingGroupCoordinator objectionWithdrawal, ContractTemplateDetail contractTemplateDetail, FileFormat fileFormat, GridOperator gridOperator) {
        log.debug("Starting file name formatting");
        List<String> fileNameParts = new ArrayList<>();
        log.debug("Extracting file prefix");
        fileNameParts.add(documentGenerationUtil.extractFilePrefix(contractTemplateDetail));
        log.debug("Extracting file name");
        fileNameParts.add(extractFileName(objectionWithdrawal, contractTemplateDetail, gridOperator));
        log.debug("Extracting file suffix");
        fileNameParts.add(extractFileSuffix(objectionWithdrawal.getCreateDate(), contractTemplateDetail));
        fileNameParts.removeIf(StringUtils::isBlank);

        String fileParts = String.join("_", fileNameParts);
        fileParts = StringUtils.isBlank(fileParts) ? "ObjectionWithdrawal" : fileParts;
        return fileParts.substring(0, Math.min(fileParts.length(), 200)).concat(".").concat(fileFormat.getSuffix()).replaceAll("/", "_");
    }

    /**
     * Extracts filename components based on template configuration.
     * Handles various filename parts like customer identifier, document number, etc.
     *
     * @param objectionWithdrawal    The withdrawal entity containing base information
     * @param contractTemplateDetail The template details containing filename configuration
     * @param gridOperator           The associated grid operator entity
     * @return Generated filename string
     */
    private String extractFileName(ObjectionWithdrawalToAChangeOfABalancingGroupCoordinator objectionWithdrawal, ContractTemplateDetail contractTemplateDetail, GridOperator gridOperator) {
        try {
            List<ContractTemplateFileName> fileName = contractTemplateDetail
                    .getFileName()
                    .stream()
                    .sorted(Comparator.comparing(ContractTemplateFileName::getPriority))
                    .toList();

            if (org.springframework.util.CollectionUtils.isEmpty(fileName)) {
                return objectionWithdrawal.getWithdrawalChangeOfCbgNumber();
            } else {
                Map<Long, Pair<Customer, CustomerDetails>> customerCache = new HashMap<>();
                List<String> nameParts = new ArrayList<>();
                nameParts.add(OBJECTION_WITHDRAWAL_LABEL);
                for (ContractTemplateFileName contractTemplateFileName : fileName) {
                    switch (contractTemplateFileName) {
                        case CUSTOMER_IDENTIFIER -> //TODO waiting for analytics
                                nameParts.add("customerIdentifierTest");
                        case CUSTOMER_NAME -> //TODO waiting for analytics
                                nameParts.add(gridOperator.getFullName());
                        case CUSTOMER_NUMBER -> //TODO waiting for analytics
                                nameParts.add("customerNumberTest");
                        case DOCUMENT_NUMBER -> nameParts.add(objectionWithdrawal.getWithdrawalChangeOfCbgNumber());
                        case FILE_ID ->
                                nameParts.add(String.valueOf(objectionWithdrawalToCbgDocFileRepository.getNextIdValue()));
                        case TIMESTAMP -> nameParts.add(objectionWithdrawal.getCreateDate().toString());
                    }
                }

                return String.join("_", nameParts);
            }
        } catch (Exception e) {
            log.error("Exception handled while trying to extract file name", e);
            return objectionWithdrawal.getWithdrawalChangeOfCbgNumber();
        }
    }

    @Override
    protected String folderPath() {
        return FOLDER_PATH;
    }
}
