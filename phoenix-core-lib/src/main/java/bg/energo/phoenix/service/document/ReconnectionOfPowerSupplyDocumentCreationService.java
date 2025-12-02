package bg.energo.phoenix.service.document;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.documentModels.disconnectionPowerSupplyRequests.DisconnectionPowerSupplyRequestsDocumentPodImpl;
import bg.energo.phoenix.model.documentModels.reconnectionOfPowerSupply.ReconnectionOfPowerSupplyDocumentModel;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.entity.nomenclature.product.GridOperator;
import bg.energo.phoenix.model.entity.receivable.reconnectionOfThePowerSupply.ReconnectionOfThePowerSupply;
import bg.energo.phoenix.model.entity.receivable.reconnectionOfThePowerSupply.ReconnectionOfThePowerSupplyDocuments;
import bg.energo.phoenix.model.entity.receivable.reconnectionOfThePowerSupply.ReconnectionPowerSupplyTemplates;
import bg.energo.phoenix.model.entity.template.ContractTemplateDetail;
import bg.energo.phoenix.model.enums.documents.DocumentSigners;
import bg.energo.phoenix.model.enums.documents.DocumentStatus;
import bg.energo.phoenix.model.enums.template.ContractTemplateFileFormat;
import bg.energo.phoenix.model.enums.template.ContractTemplateFileName;
import bg.energo.phoenix.repository.billing.companyDetails.CompanyDetailRepository;
import bg.energo.phoenix.repository.billing.companyDetails.CompanyLogoRepository;
import bg.energo.phoenix.repository.documents.DocumentsRepository;
import bg.energo.phoenix.repository.nomenclature.product.GridOperatorRepository;
import bg.energo.phoenix.repository.receivable.reconnectionOfPowerSupply.ReconnectionOfPSTemplatesRepository;
import bg.energo.phoenix.repository.receivable.reconnectionOfPowerSupply.ReconnectionOfThePowerSupplyDetailedInfoRepository;
import bg.energo.phoenix.repository.receivable.reconnectionOfPowerSupply.ReconnectionOfThePowerSupplyRepository;
import bg.energo.phoenix.repository.receivable.reconnectionOfPowerSupply.ReconnectionPowerSupplyDocumentsRepository;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static bg.energo.phoenix.exception.ErrorCode.APPLICATION_ERROR;

@Slf4j
@Service

public class ReconnectionOfPowerSupplyDocumentCreationService extends AbstractDocumentCreationService {
    private static final String FOLDER_PATH = "reconnection_document";
    public static final String RECONNECTION_POWER_SUPPLY_LABEL = "Reconnection_of_Power_Supply";
    private final ReconnectionOfThePowerSupplyRepository reconnectionOfThePowerSupplyRepository;
    private final ReconnectionOfPSTemplatesRepository reconnectionOfThePowerSupplyTemplatesRepository;
    private final ReconnectionPowerSupplyDocumentsRepository reconnectionPowerSupplyDocumentsRepository;
    private final ReconnectionOfThePowerSupplyDetailedInfoRepository reconnectionOfThePowerSupplyDetailedInfoRepository;
    private final GridOperatorRepository gridOperatorRepository;

    public ReconnectionOfPowerSupplyDocumentCreationService(
            ContractTemplateDetailsRepository contractTemplateDetailsRepository,
            ContractTemplateRepository contractTemplateRepository,
            DocumentGenerationService documentGenerationService,
            DocumentGenerationUtil documentGenerationUtil,
            DocumentsRepository documentsRepository,
            CompanyDetailRepository companyDetailRepository,
            CompanyLogoRepository companyLogoRepository,
            SignerChainManager signerChainManager,
            FileService fileService,
            ReconnectionOfThePowerSupplyRepository reconnectionOfThePowerSupplyRepository,
            ReconnectionOfPSTemplatesRepository reconnectionOfThePowerSupplyTemplatesRepository,
            ReconnectionPowerSupplyDocumentsRepository reconnectionPowerSupplyDocumentsRepository,
            ReconnectionOfThePowerSupplyDetailedInfoRepository reconnectionOfThePowerSupplyDetailedInfoRepository,
            GridOperatorRepository gridOperatorRepository
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
        this.reconnectionOfThePowerSupplyRepository = reconnectionOfThePowerSupplyRepository;
        this.reconnectionOfThePowerSupplyTemplatesRepository = reconnectionOfThePowerSupplyTemplatesRepository;
        this.reconnectionPowerSupplyDocumentsRepository = reconnectionPowerSupplyDocumentsRepository;
        this.reconnectionOfThePowerSupplyDetailedInfoRepository = reconnectionOfThePowerSupplyDetailedInfoRepository;
        this.gridOperatorRepository = gridOperatorRepository;
    }

    @Transactional
    public void generateDocuments(Long reconnectionId) {
        ReconnectionOfThePowerSupply request = reconnectionOfThePowerSupplyRepository
                .findById(reconnectionId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Reconnection of power supply not found by ID: %s".formatted(reconnectionId)));

        List<ReconnectionPowerSupplyTemplates> templates = reconnectionOfThePowerSupplyTemplatesRepository.findByPowerSupplyReconnectionId(reconnectionId);
        if (templates.isEmpty()) {
            log.info("No templates found for reconnection of power supply request ID: {}", reconnectionId);
            return;
        }

        ReconnectionOfPowerSupplyDocumentModel documentModel = buildModelWithSharedInfo(new ReconnectionOfPowerSupplyDocumentModel());
        mapReconnectionJsonTemplate(request, documentModel);

        List<Document> documentList = new ArrayList<>();

        for (ReconnectionPowerSupplyTemplates template : templates) {
            try {
                ReconnectionOfPowerSupplyDocumentCreationService.DocumentGenerationResult result = generateDocument(template, documentModel, buildFileDestinationPath(), request);
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

    private void mapReconnectionJsonTemplate(ReconnectionOfThePowerSupply request, ReconnectionOfPowerSupplyDocumentModel documentModel) {
        GridOperator gridOperator = gridOperatorRepository
                .findById(request.getGridOperatorId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Can't find grid operator with id: %s".formatted(request.getGridOperatorId())));

        List<DisconnectionPowerSupplyRequestsDocumentPodImpl> podImpl = getPodInfo(request.getId());

        documentModel.fillDocumentData(request.getReconnectionNumber(), request.getCreateDate(), gridOperator.getName(), gridOperator.getFullName(), podImpl);
    }

    private List<DisconnectionPowerSupplyRequestsDocumentPodImpl> getPodInfo(Long requestId) {
        return reconnectionOfThePowerSupplyDetailedInfoRepository
                .getPodImplForDocument(requestId).stream()
                .map(response -> {
                            var pod = new DisconnectionPowerSupplyRequestsDocumentPodImpl();
                            pod.fillPodsInfo(response);
                            return pod;
                        }
                )
                .toList();
    }

    private ReconnectionOfPowerSupplyDocumentCreationService.DocumentGenerationResult generateDocument(
            ReconnectionPowerSupplyTemplates template,
            ReconnectionOfPowerSupplyDocumentModel model,
            String destinationPath,
            ReconnectionOfThePowerSupply request
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
                    getDocumentSigners(templateDetail),
                    request.getId(),
                    template.getTemplateId()
            );
            if (ContractTemplateFileFormat.PDF.equals(format)) {
                resultDocument = savedDocument;
            }
        }

        return new ReconnectionOfPowerSupplyDocumentCreationService.DocumentGenerationResult(documentPathPayloads, templateDetail, resultDocument);
    }

    private String formatDocumentFileName(ReconnectionOfThePowerSupply requests, ContractTemplateDetail contractTemplateDetail) {
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
        fileParts = StringUtils.isBlank(fileParts) ? "POWER_SUPPLY_RECONNECTION" : fileParts;
        return fileParts.substring(0, Math.min(fileParts.length(), 200)).replaceAll("/", "_");
    }

    private String extractFileName(ReconnectionOfThePowerSupply requests, ContractTemplateDetail contractTemplateDetail) {
        try {
            List<ContractTemplateFileName> fileName = contractTemplateDetail
                    .getFileName()
                    .stream()
                    .sorted(Comparator.comparing(ContractTemplateFileName::getPriority))
                    .toList();

            if (CollectionUtils.isEmpty(fileName)) {
                return requests.getReconnectionNumber();
            } else {
                List<String> nameParts = new ArrayList<>();
                nameParts.add(RECONNECTION_POWER_SUPPLY_LABEL);
                for (ContractTemplateFileName contractTemplateFileName : fileName) {
                    switch (contractTemplateFileName) {
                        case CUSTOMER_IDENTIFIER, CUSTOMER_NAME, CUSTOMER_NUMBER -> {
                            //these fields are disabled
                        }
                        case DOCUMENT_NUMBER -> nameParts.add(requests.getReconnectionNumber());
                        case FILE_ID ->
                                nameParts.add(String.valueOf(reconnectionPowerSupplyDocumentsRepository.getNextIdValue()));
                        case TIMESTAMP -> nameParts.add(requests.getCreateDate().toString());
                    }
                }

                return String.join("_", nameParts);
            }
        } catch (Exception e) {
            log.error("Exception handled while trying to extract file name", e);
            return requests.getReconnectionNumber();
        }
    }

    private Document saveDocuments(
            String fileName,
            String ftpPath,
            FileFormat fileFormat,
            List<DocumentSigners> documentSigners,
            Long reconnectionId,
            Long templateId) {

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

        //
        reconnectionPowerSupplyDocumentsRepository.save(new ReconnectionOfThePowerSupplyDocuments(null, savedDoc.getId(), reconnectionId, EntityStatus.ACTIVE));

        return savedDoc;
    }

    @Override
    protected String folderPath() {
        return FOLDER_PATH;
    }

    private record DocumentGenerationResult(
            DocumentPathPayloads documentPaths,
            ContractTemplateDetail templateDetail,
            Document document
    ) {
    }
}
