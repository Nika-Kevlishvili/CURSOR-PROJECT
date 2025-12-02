package bg.energo.phoenix.service.receivable.balancingGroupCoordinatorObjection;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.entity.nomenclature.product.GridOperator;
import bg.energo.phoenix.model.entity.receivable.balancingGroupCoordinatorObjection.ObjectionToChangeOfCbg;
import bg.energo.phoenix.model.entity.receivable.balancingGroupCoordinatorObjection.ObjectionToChangeOfCbgDocument;
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
import bg.energo.phoenix.repository.receivable.balancingGroupCoordinatorObjection.ObjectionToChangeOfCbgDocumentRepository;
import bg.energo.phoenix.repository.receivable.balancingGroupCoordinatorObjection.ObjectionToChangeOfCbgProcessResultRepository;
import bg.energo.phoenix.repository.receivable.balancingGroupCoordinatorObjection.ObjectionToChangeOfCbgRepository;
import bg.energo.phoenix.repository.template.ContractTemplateDetailsRepository;
import bg.energo.phoenix.repository.template.ContractTemplateRepository;
import bg.energo.phoenix.service.document.AbstractDocumentCreationService;
import bg.energo.phoenix.service.document.DocumentGenerationService;
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
public class ObjectionOfCbgDocumentCreationService extends AbstractDocumentCreationService {
    private static final String FOLDER_PATH = "objection_to_change_of_cbg_document";
    private static final String CHANGE_OF_COORDINATOR_OBJECTION_LABEL = "Change_of_Coordinator_Objection";
    private final ObjectionToChangeOfCbgProcessResultRepository objectionToChangeOfCbgProcessResultRepository;
    private final ObjectionToChangeOfCbgRepository objectionToChangeOfCbgRepository;
    private final GridOperatorRepository gridOperatorRepository;
    private final ObjectionToChangeOfCbgDocumentRepository objectionToChangeOfCbgDocumentRepository;

    public ObjectionOfCbgDocumentCreationService(
            ContractTemplateDetailsRepository contractTemplateDetailsRepository,
            ContractTemplateRepository contractTemplateRepository,
            DocumentGenerationService documentGenerationService,
            DocumentGenerationUtil documentGenerationUtil,
            DocumentsRepository documentsRepository,
            CompanyDetailRepository companyDetailRepository,
            CompanyLogoRepository companyLogoRepository,
            SignerChainManager signerChainManager,
            FileService fileService,
            ObjectionToChangeOfCbgProcessResultRepository objectionToChangeOfCbgProcessResultRepository,
            ObjectionToChangeOfCbgRepository objectionToChangeOfCbgRepository,
            GridOperatorRepository gridOperatorRepository,
            ObjectionToChangeOfCbgDocumentRepository objectionToChangeOfCbgDocumentRepository
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
        this.objectionToChangeOfCbgProcessResultRepository = objectionToChangeOfCbgProcessResultRepository;
        this.objectionToChangeOfCbgRepository = objectionToChangeOfCbgRepository;
        this.gridOperatorRepository = gridOperatorRepository;
        this.objectionToChangeOfCbgDocumentRepository = objectionToChangeOfCbgDocumentRepository;
    }

    @Transactional
    public ObjectionDocumentGenerationResponse generateDocument(Long cbgId, DocumentGenerationType type) {
        List<Long> documentIds = new ArrayList<>();
        List<Document> documentsToSign = new ArrayList<>();
        List<ObjectionToChangeOfCbgDocument> documentFiles = new ArrayList<>();
        ObjectionDocumentGenerationResponse documentResponse = new ObjectionDocumentGenerationResponse();

        ObjectionOfCbgDocumentImpl model = buildModelWithSharedInfo(new ObjectionOfCbgDocumentImpl());

        ObjectionToChangeOfCbg objection = objectionToChangeOfCbgRepository
                .findById(cbgId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Can't find Objection with id: %s ".formatted(cbgId)));

        GridOperator gridOperator = gridOperatorRepository
                .findById(objection.getGridOperatorId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Can't find grid operator with id: %s".formatted(objection.getGridOperatorId())));

        mapDataIntoModel(objection, model, gridOperator);

        if (type.equals(DocumentGenerationType.EMAIL_TEMPLATE)) {
            handleEmailTemplateGeneration(objection, model, documentResponse, gridOperator);
        } else {
            handleDocumentTemplateGeneration(objection, model, gridOperator, documentIds, documentsToSign, documentFiles);
        }

        if (!documentsToSign.isEmpty()) {
            signerChainManager.startSign(documentsToSign);

            for (int i = 0; i < documentsToSign.size(); i++) {
                Document doc = documentsToSign.get(i);
                ObjectionToChangeOfCbgDocument docFile = documentFiles.get(i);
                docFile.setFileUrl(doc.getSignedFileUrl());
            }
            objectionToChangeOfCbgDocumentRepository.saveAll(documentFiles);
        }

        documentResponse.setDocumentIds(documentIds);
        return documentResponse;
    }

    private void handleEmailTemplateGeneration(
            ObjectionToChangeOfCbg objection,
            ObjectionOfCbgDocumentImpl model,
            ObjectionDocumentGenerationResponse documentResponse,
            GridOperator gridOperator
    ) {
        try {
            ContractTemplateDetail templateDetail = getContractTemplateLastDetails(objection.getEmailTemplateId());

            ByteArrayResource emailTemplateFileResource = createTemplateFileResource(templateDetail);

            String fileName = formatDocumentFileName(objection, templateDetail, FileFormat.DOCX, gridOperator);

            DocumentPathPayloads documentPathPayloads = documentGenerationService.generateDocument(
                    emailTemplateFileResource,
                    buildFileDestinationPath(),
                    fileName,
                    model,
                    Set.of(FileFormat.DOCX),
                    false
            );

            ObjectionToChangeOfCbgDocument document = objectionToChangeOfCbgDocumentRepository.save(
                    ObjectionToChangeOfCbgDocument.builder()
                            .fileUrl(documentPathPayloads.docXPath())
                            .templateId(templateDetail.getTemplateId())
                            .fileName(fileName)
                            .status(EntityStatus.ACTIVE)
                            .changeOfCbgId(objection.getId())
                            .build()
            );

            documentResponse.setEmailDocumentId(Collections.singletonMap(document.getId(), templateDetail.getSubject()));
        } catch (Exception e) {
            log.error("Exception while storing template files: {}", e.getMessage());
            throw new ClientException("Exception while storing template files: %s".formatted(e.getMessage()), APPLICATION_ERROR);
        }
    }

    private void handleDocumentTemplateGeneration(
            ObjectionToChangeOfCbg objection,
            ObjectionOfCbgDocumentImpl model,
            GridOperator gridOperator,
            List<Long> documentIds,
            List<Document> documentsToSign,
            List<ObjectionToChangeOfCbgDocument> documentFiles
    ) {
        List<ContractTemplate> documentTemplateList = objectionToChangeOfCbgRepository.findRelatedContractTemplates(objection.getId());

        for (ContractTemplate template : documentTemplateList) {
            ContractTemplateDetail templateDetail = getContractTemplateDetails(template.getLastTemplateDetailId());

            List<DocumentSigners> documentSigners = getDocumentSigners(templateDetail);

            try {
                ByteArrayResource documentTemplateFileResource = createTemplateFileResource(templateDetail);

                for (ContractTemplateFileFormat format : templateDetail.getOutputFileFormat()) {
                    FileFormat fileFormat = FileFormat.fromContractTemplateFileFormat(format);
                    String fileName = formatDocumentFileName(objection, templateDetail, fileFormat, gridOperator);

                    DocumentPathPayloads documentPathPayloads = documentGenerationService.generateDocument(
                            documentTemplateFileResource,
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

                    ObjectionToChangeOfCbgDocument documentFile = objectionToChangeOfCbgDocumentRepository.save(
                            ObjectionToChangeOfCbgDocument.builder()
                                    .fileUrl(fileUrl)
                                    .templateId(templateDetail.getTemplateId())
                                    .fileName(fileName)
                                    .status(EntityStatus.ACTIVE)
                                    .changeOfCbgId(objection.getId())
                                    .documentId(document.getId())
                                    .build()
                    );
                    documentIds.add(documentFile.getId());

                    if (FileFormat.PDF.equals(fileFormat)) {
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

    private void mapDataIntoModel(
            ObjectionToChangeOfCbg objection,
            ObjectionOfCbgDocumentImpl model,
            GridOperator gridOperator
    ) {
        List<ObjectionOfCbgDocumentPodImpl> podImpl = objectionToChangeOfCbgProcessResultRepository.getPodImpl(objection.getId());

        model.fillObjectionData(
                objection.getChangeOfCbgNumber(),
                gridOperator.getName(),
                objection.getCreateDate(),
                objection.getChangeDate(),
                podImpl
        );
    }

    public String formatDocumentFileName(
            ObjectionToChangeOfCbg objection,
            ContractTemplateDetail contractTemplateDetail,
            FileFormat fileFormat,
            GridOperator gridOperator
    ) {
        log.debug("Starting file name formatting");
        List<String> fileNameParts = new ArrayList<>();
        log.debug("Extracting file prefix");
        fileNameParts.add(documentGenerationUtil.extractFilePrefix(contractTemplateDetail));
        log.debug("Extracting file name");
        fileNameParts.add(extractFileName(objection, contractTemplateDetail, gridOperator));
        log.debug("Extracting file suffix");
        fileNameParts.add(extractFileSuffix(objection.getCreateDate(), contractTemplateDetail));
        fileNameParts.removeIf(StringUtils::isBlank);
        String fileParts = String.join("_", fileNameParts);
        fileParts = StringUtils.isBlank(fileParts) ? "Objection" : fileParts;
        return fileParts.substring(0, Math.min(fileParts.length(), 200))
                .concat(".")
                .concat(fileFormat.getSuffix())
                .replaceAll("/", "_");
    }

    private String extractFileName(
            ObjectionToChangeOfCbg objection,
            ContractTemplateDetail contractTemplateDetail,
            GridOperator gridOperator
    ) {
        try {
            List<ContractTemplateFileName> fileName = contractTemplateDetail
                    .getFileName()
                    .stream()
                    .sorted(Comparator.comparing(ContractTemplateFileName::getPriority))
                    .toList();

            if (org.springframework.util.CollectionUtils.isEmpty(fileName)) {
                return objection.getChangeOfCbgNumber();
            } else {
                Map<Long, Pair<Customer, CustomerDetails>> customerCache = new HashMap<>();
                List<String> nameParts = new ArrayList<>();
                nameParts.add(CHANGE_OF_COORDINATOR_OBJECTION_LABEL);
                for (ContractTemplateFileName contractTemplateFileName : fileName) {
                    switch (contractTemplateFileName) {
                        case CUSTOMER_IDENTIFIER -> //TODO waiting for analytics
                                nameParts.add("customerIdentifierTest");
                        case CUSTOMER_NAME -> //TODO waiting for analytics
                                nameParts.add(gridOperator.getFullName());
                        case CUSTOMER_NUMBER -> //TODO waiting for analytics
                                nameParts.add("customerNumberTest");
                        case DOCUMENT_NUMBER -> nameParts.add(objection.getChangeOfCbgNumber());
                        case FILE_ID ->
                                nameParts.add(String.valueOf(objectionToChangeOfCbgDocumentRepository.getNextIdValue()));
                        case TIMESTAMP -> nameParts.add(objection.getCreateDate().toString());
                    }
                }

                return String.join("_", nameParts);
            }
        } catch (Exception e) {
            log.error("Exception handled while trying to extract file name", e);
            return objection.getChangeOfCbgNumber();
        }
    }

    @Override
    protected String folderPath() {
        return FOLDER_PATH;
    }
}