package bg.energo.phoenix.service.contract.product;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.exception.IllegalArgumentsProvidedException;
import bg.energo.phoenix.model.documentModels.contract.*;
import bg.energo.phoenix.model.documentModels.contract.response.*;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.product.ProductContract;
import bg.energo.phoenix.model.entity.contract.product.ProductContractDetails;
import bg.energo.phoenix.model.entity.contract.product.ProductContractSignableDocuments;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.entity.template.ContractTemplateDetail;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailsStatus;
import bg.energo.phoenix.model.enums.contract.products.ProductContractStatus;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.documents.DocumentSigners;
import bg.energo.phoenix.model.enums.documents.DocumentStatus;
import bg.energo.phoenix.model.enums.template.ContractTemplateFileName;
import bg.energo.phoenix.model.enums.template.ContractTemplatePurposes;
import bg.energo.phoenix.model.enums.template.ContractTemplateSigning;
import bg.energo.phoenix.model.request.communication.edms.Attribute;
import bg.energo.phoenix.model.request.contract.product.ContractDocumentSaveRequest;
import bg.energo.phoenix.model.response.contract.DocumentGenerationPopupResponse;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.repository.billing.companyDetails.CompanyDetailRepository;
import bg.energo.phoenix.repository.billing.companyDetails.CompanyLogoRepository;
import bg.energo.phoenix.repository.contract.billing.ContractPodRepository;
import bg.energo.phoenix.repository.contract.product.*;
import bg.energo.phoenix.repository.contract.proxy.ProxyManagersRepository;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.customer.ManagerRepository;
import bg.energo.phoenix.repository.documents.DocumentsRepository;
import bg.energo.phoenix.repository.product.product.ProductPriceComponentRepository;
import bg.energo.phoenix.repository.template.ContractTemplateDetailsRepository;
import bg.energo.phoenix.repository.template.ContractTemplateRepository;
import bg.energo.phoenix.repository.template.ProductTemplateRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.archivation.edms.FileArchivationService;
import bg.energo.phoenix.service.archivation.edms.config.EDMSAttributeProperties;
import bg.energo.phoenix.service.document.AbstractDocumentCreationService;
import bg.energo.phoenix.service.document.DocumentGenerationService;
import bg.energo.phoenix.service.document.enums.DocumentPathPayloads;
import bg.energo.phoenix.service.document.enums.FileFormat;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.service.signing.SignerChainManager;
import bg.energo.phoenix.util.archivation.EDMSArchivationConstraints;
import bg.energo.phoenix.util.contract.ContractDocumentTranslationUtil;
import bg.energo.phoenix.util.template.document.DocumentGenerationUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static bg.energo.phoenix.exception.ErrorCode.APPLICATION_ERROR;

@Service
@Slf4j
public class ProductContractDocumentCreationService extends AbstractDocumentCreationService {

    private final ProductContractRepository productContractRepository;
    private final ManagerRepository managerRepository;
    private final ProxyManagersRepository proxyManagersRepository;
    private final ProductContractInterimAdvancePaymentsRepository productContractInterimAdvancePaymentsRepository;
    private final ContractPodRepository contractPodRepository;
    private final ProductPriceComponentRepository productPriceComponentRepository;
    private final ProductTemplateRepository productTemplateRepository;
    private final ProductContractDetailsRepository productContractDetailsRepository;
    private final CustomerRepository customerRepository;
    private final CustomerDetailsRepository customerDetailsRepository;
    private final ProductContractFileRepository productContractFileRepository;
    private final ProductContractSignableDocumentRepository signableDocumentRepository;
    private final PermissionService permissionService;
    private final EDMSAttributeProperties edmsAttributeProperties;
    private final FileArchivationService fileArchivationService;
    @Value("${product_contract.document.ftp_directory_path}")
    private String productContractDocumentFtpPath;
    private final ContractDocumentTranslationUtil translationUtil;

    public ProductContractDocumentCreationService(
            ContractTemplateDetailsRepository contractTemplateDetailsRepository,
            ContractTemplateRepository contractTemplateRepository,
            DocumentGenerationService documentGenerationService,
            DocumentGenerationUtil documentGenerationUtil,
            DocumentsRepository documentsRepository,
            CompanyDetailRepository companyDetailRepository,
            CompanyLogoRepository companyLogoRepository,
            SignerChainManager signerChainManager,
            FileService fileService,
            ProductContractRepository productContractRepository,
            ManagerRepository managerRepository,
            ProxyManagersRepository proxyManagersRepository,
            ProductContractInterimAdvancePaymentsRepository productContractInterimAdvancePaymentsRepository,
            ContractPodRepository contractPodRepository,
            ProductPriceComponentRepository productPriceComponentRepository,
            ProductTemplateRepository productTemplateRepository,
            ProductContractDetailsRepository productContractDetailsRepository,
            CustomerRepository customerRepository,
            CustomerDetailsRepository customerDetailsRepository,
            ProductContractFileRepository productContractFileRepository,
            ProductContractSignableDocumentRepository signableDocumentRepository,
            PermissionService permissionService,
            EDMSAttributeProperties edmsAttributeProperties,
            FileArchivationService fileArchivationService,
            ContractDocumentTranslationUtil translationUtil
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
        this.productContractRepository = productContractRepository;
        this.managerRepository = managerRepository;
        this.proxyManagersRepository = proxyManagersRepository;
        this.productContractInterimAdvancePaymentsRepository = productContractInterimAdvancePaymentsRepository;
        this.contractPodRepository = contractPodRepository;
        this.productPriceComponentRepository = productPriceComponentRepository;
        this.productTemplateRepository = productTemplateRepository;
        this.productContractDetailsRepository = productContractDetailsRepository;
        this.customerRepository = customerRepository;
        this.customerDetailsRepository = customerDetailsRepository;
        this.productContractFileRepository = productContractFileRepository;
        this.signableDocumentRepository = signableDocumentRepository;
        this.permissionService = permissionService;
        this.edmsAttributeProperties = edmsAttributeProperties;
        this.translationUtil = translationUtil;
        this.fileArchivationService = fileArchivationService;
    }

    /**
     * Retrieves the document generation popup for a given product contract and version.
     *
     * @param id        The ID of the product contract.
     * @param versionId The version ID of the product contract.
     * @return A list of {@link DocumentGenerationPopupResponse} objects containing the details of the available document templates.
     * @throws ClientException               if the user does not have the necessary permissions to generate documents for the contract.
     * @throws DomainEntityNotFoundException if the contract or contract version cannot be found, or if the contract has an invalid status for document generation.
     */
    public List<DocumentGenerationPopupResponse> getDocumentGenerationPopup(Long id, Integer versionId) {
        if (!permissionService.permissionContextContainsPermissions(PermissionContextEnum.PRODUCT_CONTRACTS, List.of(PermissionEnum.PRODUCT_CONTRACT_GENERATE))) {
            throw new ClientException("You can not generate documents for this contract!;", ErrorCode.ACCESS_DENIED);
        }

        if (!productContractRepository.existsByIdAndStatusAndContractStatusNotIn(id, ProductContractStatus.ACTIVE, List.of(ContractDetailsStatus.TERMINATED, ContractDetailsStatus.CANCELLED))) {
            throw new DomainEntityNotFoundException("id-Contract can't be found or has wrong status for generation;");
        }

        if (!productContractDetailsRepository.existsByContractIdAndVersionId(id, versionId)) {
            throw new DomainEntityNotFoundException("versionId-version not found!;");
        }

        return productTemplateRepository
                .findTemplatesForContractDocumentGeneration(id, versionId, getTemplatePurposes())
                .stream()
                .map(response ->
                        new DocumentGenerationPopupResponse(
                                response.getTemplateId(),
                                response.getTemplateVersion(), response.getTemplateName(),
                                response.getOutputFileFormat(), response.getFileSignings()
                        )
                )
                .toList();
    }

    /**
     * Generates documents for a given product contract and version.
     * <p>
     * This method is responsible for generating documents based on the provided request. It performs the following steps:
     * 1. Checks if the user has the necessary permissions to generate documents for the contract.
     * 2. Retrieves the product contract and contract details based on the provided ID and version.
     * 3. Validates the requested document templates and their associated file formats and signing options.
     * 4. Generates the requested documents using the provided templates and saves them to the configured FTP path.
     * 5. Deletes any previous files associated with the contract details.
     *
     * @param request The {@link ContractDocumentSaveRequest} containing the details of the documents to be generated.
     * @throws ClientException                   if the user does not have the necessary permissions to generate documents for the contract.
     * @throws DomainEntityNotFoundException     if the contract or contract version cannot be found, or if the contract has an invalid status for document generation.
     * @throws IllegalArgumentsProvidedException if any of the requested document templates or their associated file formats and signing options are invalid.
     */
    @Transactional
    public void generateDocument(ContractDocumentSaveRequest request) {
        Long id = request.getContractId();
        Integer versionId = Math.toIntExact(request.getVersionId());

        if (!permissionService.permissionContextContainsPermissions(PermissionContextEnum.PRODUCT_CONTRACTS, List.of(PermissionEnum.PRODUCT_CONTRACT_GENERATE))) {
            throw new ClientException("You can not generate documents for this contract!;", ErrorCode.ACCESS_DENIED);
        }

        ProductContract productContract = productContractRepository
                .findByIdAndStatusAndContractStatusIsNotIn(id, ProductContractStatus.ACTIVE, List.of(ContractDetailsStatus.CANCELLED, ContractDetailsStatus.TERMINATED))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Product contract not found or has invalid status for document generation!"));

        ProductContractDetails productContractDetails = productContractDetailsRepository
                .findByContractIdAndVersionId(id, versionId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Product contract details not found"));

        ContractDocumentModel contractDocumentModel = fetchDocumentModel(id, versionId);

        List<ContractDocumentSaveRequest.DocumentSaveRequestModels> documents = request.getDocuments();
        List<Long> list = documents.stream().map(ContractDocumentSaveRequest.DocumentSaveRequestModels::getTemplateId).toList();
        Set<Long> fetchedTemplateIds = productTemplateRepository.findForContractDocument(id, versionId, list, getTemplatePurposes());
        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i < documents.size(); i++) {
            Long templateId = documents.get(i).getTemplateId();
            if (!fetchedTemplateIds.contains(templateId)) {
                stringBuilder.append("documents[%s].templateId-Product does not have provided template with id %s!;".formatted(i, templateId));
            }
        }

        if (!stringBuilder.isEmpty()) {
            throw new IllegalArgumentsProvidedException(stringBuilder.toString());
        }
        Set<ContractTemplateDetail> templateDetails = contractTemplateDetailsRepository.findRespectiveTemplateDetailsByTemplateIds(fetchedTemplateIds);
        Map<Long, ContractTemplateDetail> templateDetailMap = templateDetails.stream().collect(Collectors.toMap(ContractTemplateDetail::getTemplateId, j -> j));
        for (int i = 0; i < documents.size(); i++) {
            ContractDocumentSaveRequest.DocumentSaveRequestModels document = documents.get(i);
            Long templateId = document.getTemplateId();

            if (!templateDetailMap.get(templateId).getOutputFileFormat().stream()
                    .map(FileFormat::fromContractTemplateFileFormat).collect(Collectors.toSet())
                    .containsAll(document.getOutputFileFormat())) {
                stringBuilder.append("documents[%s].outputFileFormat-Template with id %s does not contain all provided output file formats!;".formatted(i, templateId));
            }
            List<ContractTemplateSigning> fileSignings = templateDetailMap.get(templateId).getFileSigning();
            if (CollectionUtils.isNotEmpty(fileSignings) && CollectionUtils.isNotEmpty(document.getSignings())
                    && !new HashSet<>(fileSignings).containsAll(document.getSignings())) {
                stringBuilder.append("documents[%s].signings-Template with id %s does not contain all provided signing options!;".formatted(i, templateId));
            }

        }

        if (!stringBuilder.isEmpty()) {
            throw new IllegalArgumentsProvidedException(stringBuilder.toString());
        }

        generateDocument(documents, templateDetailMap, productContract, productContractDetails, contractDocumentModel);
    }

    public Optional<ByteArrayResource> generateEmailDocument(Long id, Integer versionId, Long emailTemplateId) {
        try {
            log.debug("start fetching document model for contract id {}, version {}", id, versionId);
            ContractDocumentModel contractDocumentModel = fetchDocumentModel(id, versionId);
            log.debug("end fetching document model for contract id {}, version {}", id, versionId);

            LocalDate currentDate = LocalDate.now();
            Optional<ContractTemplateDetail> templateDetailOptional = contractTemplateDetailsRepository
                    .findRespectiveTemplateDetailsByTemplateIdAndDate(emailTemplateId, currentDate);

            if (templateDetailOptional.isEmpty()) {
                log.error("Template detail not found for Email Template ID: [{}], Date: [{}]", emailTemplateId, currentDate);
                return Optional.empty();
            }

            ContractTemplateDetail contractTemplateDetail = templateDetailOptional.get();
            String destinationPath = "%s/%s/%s".formatted(productContractDocumentFtpPath, "email_documents", currentDate);

            ByteArrayResource templateFileResource = createTemplateFileResource(contractTemplateDetail);
            log.debug("Template file cloned successfully.");
            String fileName = UUID.randomUUID().toString().concat(".docx");
            DocumentPathPayloads documentPathPayloads = documentGenerationService.generateDocument(
                    templateFileResource,
                    destinationPath,
                    fileName,
                    contractDocumentModel,
                    Set.of(FileFormat.DOCX),
                    false
            );
            log.debug("Document created successfully.");

            ByteArrayResource downloadedFile = getFileService().downloadFile(documentPathPayloads.docXPath());
            log.debug("file downloaded to generate email body, contract id {}, version {}", id, versionId);
            return Optional.ofNullable(downloadedFile);
        } catch (DomainEntityNotFoundException ex) {
            log.error("Entity not found: {}", ex.getMessage());
        } catch (IOException ex) {
            log.error("IO error while processing template file: {}", ex.getMessage(), ex);
        } catch (Exception ex) {
            log.error("Unexpected error: {}", ex.getMessage(), ex);
        }

        return Optional.empty();
    }

    private void generateDocument(List<ContractDocumentSaveRequest.DocumentSaveRequestModels> documents, Map<Long, ContractTemplateDetail> templateDetailMap, ProductContract productContract, ProductContractDetails productContractDetails, ContractDocumentModel contractDocumentModel) {
        log.debug("deleting previous files");
        deletePreviousFiles(documents, productContractDetails);

        LocalDate currentDate = LocalDate.now();
        List<Document> documentList = new ArrayList<>();
        for (ContractDocumentSaveRequest.DocumentSaveRequestModels document : documents) {
            ContractTemplateDetail template = templateDetailMap.get(document.getTemplateId());
            String destinationPath = "%s/%s".formatted(productContractDocumentFtpPath, currentDate.toString());
            log.debug("destination path for template file: {}", destinationPath);

            ByteArrayResource templateFileResource;
            try {
                templateFileResource = new ByteArrayResource(Files.readAllBytes(new File(documentGenerationUtil.getTemplateFileLocalPath(template)).toPath()));
                log.debug("Clone of template file created");

                String fileName = formatFileName(productContract, productContractDetails, template);
                log.debug("File name was formatted: [%s]".formatted(fileName));

                log.debug("Generating document");
                DocumentPathPayloads documentPathPayloads = documentGenerationService
                        .generateDocument(
                                templateFileResource,
                                destinationPath,
                                fileName,
                                contractDocumentModel,
                                new HashSet<>(document.getOutputFileFormat()),
                                productContract.getContractStatus() == ContractDetailsStatus.DRAFT
                        );
                log.debug("Documents was created successfully");

                log.debug("Saving product contract document entity");

                for (FileFormat format : document.getOutputFileFormat()) {
                    String generatedDocumentPath = documentGenerationUtil.getGeneratedDocumentPath(format, documentPathPayloads);
                    saveDocuments(
                            String.format("%s.%s", fileName, format.suffix),
                            generatedDocumentPath,
                            format,
                            getDocumentSigners(document),
                            productContractDetails.getId(),
                            document.getTemplateId(),
                            documentList,
                            productContract.getContractNumber()
                    );
                }

            } catch (Exception e) {
                log.error("exception while storing template file: {}", e.getMessage());
                throw new ClientException("exception while storing template file: %s".formatted(e.getMessage()), APPLICATION_ERROR);
            }
        }

        List<ContractDetailsStatus> validStatusesForSigning = List.of(
                ContractDetailsStatus.READY,
                ContractDetailsStatus.SIGNED,
                ContractDetailsStatus.ENTERED_INTO_FORCE,
                ContractDetailsStatus.ACTIVE_IN_TERM,
                ContractDetailsStatus.ACTIVE_IN_PERPETUITY,
                ContractDetailsStatus.CHANGED_WITH_AGREEMENT
        );

        if (validStatusesForSigning.contains(productContract.getContractStatus())) {
            signerChainManager.startSign(documentList);
        }
    }

    private void setParamsAndAttributesToDocument(Document document, String contractNumber) {
        document.setArchivationConstraints(EDMSArchivationConstraints.DOCUMENT_TYPE_SERVICE_CONTRACT_GENERATED_DOCUMENT);
        document.setArchivedFileType(EDMSArchivationConstraints.DOCUMENT_TYPE_SERVICE_CONTRACT_GENERATED_DOCUMENT.getValue());
        document.setAttributes(
                List.of(
                        new Attribute(edmsAttributeProperties.getDocumentTypeGuid(), EDMSArchivationConstraints.DOCUMENT_TYPE_SERVICE_CONTRACT_GENERATED_DOCUMENT),
                        new Attribute(edmsAttributeProperties.getDocumentNumberGuid(), contractNumber),
                        new Attribute(edmsAttributeProperties.getDocumentDateGuid(), LocalDateTime.now()),
                        new Attribute(edmsAttributeProperties.getCustomerIdentifierGuid(), ""),
                        new Attribute(edmsAttributeProperties.getCustomerNumberGuid(), ""),
                        new Attribute(edmsAttributeProperties.getSignedGuid(), false)
                )
        );
        document.setSignedFile(false);
    }

    private List<String> getTemplatePurposes() {
        List<String> purposes = new ArrayList<>();
        if (permissionService.permissionContextContainsPermissions(PermissionContextEnum.PRODUCT_CONTRACTS, List.of(PermissionEnum.PRODUCT_CONTRACT_GENERATE_ADDITIONAL_AGREEMENT_BASE))) {
            purposes.add(ContractTemplatePurposes.PRODUCT_ADDITIONAL_TEMPLATE_BASE.name());
        }
        if (permissionService.permissionContextContainsPermissions(PermissionContextEnum.PRODUCT_CONTRACTS, List.of(PermissionEnum.PRODUCT_CONTRACT_GENERATE_ADDITIONAL_AGREEMENT_ADVANCE))) {
            purposes.add(ContractTemplatePurposes.PRODUCT_ADDITIONAL_TEMPLATE_ADVANCE.name());
        }
        return purposes;
    }

    private void deletePreviousFiles(List<ContractDocumentSaveRequest.DocumentSaveRequestModels> documents, ProductContractDetails productContractDetails) {
        List<Long> templateIds = documents.stream().filter(ContractDocumentSaveRequest.DocumentSaveRequestModels::isDeletePreviousFiles).map(ContractDocumentSaveRequest.DocumentSaveRequestModels::getTemplateId).toList();
        List<Document> documentPreviousFiles = signableDocumentRepository.getDocumentPreviousFiles(productContractDetails.getId(), templateIds);
        documentPreviousFiles.forEach(document -> document.setStatus(EntityStatus.DELETED));
        documentsRepository.saveAllAndFlush(documentPreviousFiles);
    }

    private List<DocumentSigners> getDocumentSigners(ContractDocumentSaveRequest.DocumentSaveRequestModels document) {
        return Optional.ofNullable(document.getSignings())
                .orElse(Collections.emptyList())
                .stream()
                .map(ContractTemplateSigning::getDocumentSigners)
                .toList();
    }


    private void saveDocuments(
            String fileName,
            String ftpPath,
            FileFormat fileFormat,
            List<DocumentSigners> documentSigners,
            Long contractDetailId,
            Long templateId,
            List<Document> documentList,
            String contractNumber) {

        if (ftpPath == null) {
            return;
        }
        //Todo Some changes will be added in future
        Document document = Document.builder()
                .signers(FileFormat.PDF.equals(fileFormat) ? documentSigners : new ArrayList<>())
                .signedBy(new ArrayList<>())
                .name(fileName)
                .unsignedFileUrl(ftpPath)
                .signedFileUrl(ftpPath)
                .fileFormat(fileFormat)
                .templateId(templateId)
                .documentStatus(FileFormat.PDF.equals(fileFormat) ? DocumentStatus.UNSIGNED : DocumentStatus.SIGNED)
                .status(EntityStatus.ACTIVE)
                .needArchive(true)
                .build();
        documentsRepository.saveAndFlush(document);
        signableDocumentRepository.save(new ProductContractSignableDocuments(null, document.getId(), contractDetailId, false, EntityStatus.ACTIVE));
        setParamsAndAttributesToDocument(document, contractNumber);
        fileArchivationService.archiveDocument(document);
        documentsRepository.saveAndFlush(document);

        if (FileFormat.PDF.equals(fileFormat)) {
            document.setSignedFile(true);
            List<Attribute> attributes = document.getAttributes();
            if (!attributes.isEmpty()) {
                attributes.stream()
                        .filter(it -> it.getAttributeGuid().equals(edmsAttributeProperties.getSignedGuid()))
                        .findFirst()
                        .ifPresent(it -> it.setValue(true));
                document.setAttributes(attributes);
            }
            documentsRepository.saveAndFlush(document);
            documentList.add(document);
        }

    }

    private String formatFileName(ProductContract contract, ProductContractDetails contractDetails, ContractTemplateDetail
            contractTemplateDetail) {
        log.debug("Starting file name formatting");
        List<String> fileNameParts = new ArrayList<>();
        log.debug("Extracting file prefix");
        fileNameParts.add(ObjectUtils.defaultIfNull(contractTemplateDetail.getFileNamePrefix(), ""));
        log.debug("Extracting file name");
        fileNameParts.add(extractFileName(contract, contractDetails, contractTemplateDetail));
        log.debug("Extracting file suffix");
        fileNameParts.add(extractFileSuffix(contract.getCreateDate(), contractTemplateDetail));
        fileNameParts.removeIf(StringUtils::isBlank);

        String fileParts = String.join("_", fileNameParts);
        fileParts = StringUtils.isBlank(fileParts) ? "Product_Contract" : fileParts;
        return fileParts.substring(0, Math.min(fileParts.length(), 200)).replaceAll("/", "_");
    }

    private String extractFileName(ProductContract contract, ProductContractDetails contractDetails, ContractTemplateDetail contractTemplateDetail) {
        try {
            List<ContractTemplateFileName> fileName = contractTemplateDetail
                    .getFileName()
                    .stream()
                    .sorted(Comparator.comparing(ContractTemplateFileName::getPriority))
                    .toList();

            if (org.springframework.util.CollectionUtils.isEmpty(fileName)) {
                return contract.getContractNumber();
            } else {
                Map<Long, Pair<Customer, CustomerDetails>> customerCache = new HashMap<>();
                List<String> nameParts = new ArrayList<>();

                for (ContractTemplateFileName contractTemplateFileName : fileName) {
                    switch (contractTemplateFileName) {
                        case CUSTOMER_IDENTIFIER -> {
                            nameParts.add(getCustomer(contractDetails.getCustomerDetailId(), customerCache).getKey().getIdentifier());
                        }
                        case CUSTOMER_NAME -> {
                            Pair<Customer, CustomerDetails> customerPair = getCustomer(contractDetails.getCustomerDetailId(), customerCache);
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
                            Pair<Customer, CustomerDetails> customerPair = getCustomer(contractDetails.getCustomerDetailId(), customerCache);
                            Customer customer = customerPair.getKey();

                            nameParts.add(String.valueOf(customer.getCustomerNumber()));
                        }
                        case DOCUMENT_NUMBER -> nameParts.add(contract.getContractNumber());
                        case FILE_ID -> nameParts.add(String.valueOf(productContractFileRepository.getNextIdValue()));
                        case TIMESTAMP -> nameParts.add(contract.getCreateDate().toString());
                    }
                }

                return String.join("_", nameParts);
            }
        } catch (Exception e) {
            log.error("Exception handled while trying to extract file name", e);
            return contract.getContractNumber();
        }
    }

    private Pair<Customer, CustomerDetails> getCustomer(Long customerDetailId, Map<Long, Pair<Customer, CustomerDetails>> customerCache) {
        if (!customerCache.containsKey(customerDetailId)) {
            Customer customer = customerRepository
                    .findByCustomerDetailIdAndStatusIn(customerDetailId, Arrays.stream(CustomerStatus.values()).toList())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Customer with detail id: [%s] not found;".formatted(customerDetailId)));

            CustomerDetails customerDetails = customerDetailsRepository
                    .findById(customerDetailId)
                    .orElseThrow(() -> new DomainEntityNotFoundException("Customer with detail id: [%s] not found;".formatted(customerDetailId)));

            customerCache.put(customerDetailId, Pair.of(customer, customerDetails));
        }

        return Pair.of(customerCache.get(customerDetailId).getKey(), customerCache.get(customerDetailId).getValue());
    }


    public ContractDocumentModel fetchDocumentModel(Long id, Integer versionId) {
        log.debug("fetching main contract info, id {}", id);
        ContractMainResponse contractMainResponse = productContractRepository.fetchContractInfoForDocument(id, versionId);
        log.debug("finished fetching main contract info, id {}", id);
        List<ManagerModel> managers = getManagerModels(id, versionId);

        log.debug("fetching interims, id {}", id);
        List<InterimAdvancePaymentDetailModel> interims = getInterimAdvancePaymentDetailModels(id, versionId);
        log.debug("finished fetching interims, id {}", id);

        log.debug("fetching pods, id {}", id);
        List<PodResponse> allPods = contractPodRepository.fetchVersionPodsForDocument(id, versionId);
        log.debug("finished fetching pods, id {}", id);
        List<PodModel> versionAddedPods = getVersionAddedPods(allPods);
        List<PodModel> versionPods = getVersionPods(allPods);
        List<PodModel> versionRemovedPods = getVersionRemovedPods(allPods);

        log.debug("start pod translation, id {}", id);
        translationUtil.translatePods(versionAddedPods);
        translationUtil.translatePods(versionPods);
        translationUtil.translatePods(versionRemovedPods);
        log.debug("finished pod translation, id {}", id);

        log.debug("fetching price components, id {}", id);
        List<PriceComponentModel> priceComponentModels = getPriceComponentModels(id, versionId);
        log.debug("finished fetching price components, id {}", id);

        log.debug("mapping main contract info, id {}", id);
        ContractDocumentModel documentModel = new ContractDocumentModel().from(contractMainResponse, managers, interims, versionPods, versionAddedPods, versionRemovedPods, priceComponentModels);
        log.debug("finished mapping main contract info, id {}", id);

        log.debug("translate model, id {}", id);
        translationUtil.translateModel(documentModel);
        log.debug("finished translating model, id {}", id);

        ContractDocumentModel contractDocumentModel = buildModelWithSharedInfo(documentModel);
        log.debug("finished building main contract with shared info, id {}", id);
        return contractDocumentModel;
    }

    private List<PriceComponentModel> getPriceComponentModels(Long id, Integer versionId) {
        List<PriceComponentModel> priceComponentModels = new ArrayList<>();
        List<PriceComponentResponse> priceComponentResponses = productPriceComponentRepository.fetchPriceComponentsForDocument(id, versionId);
        priceComponentResponses.forEach(pc -> priceComponentModels.add(new PriceComponentModel().from(pc)));
        return priceComponentModels;
    }

    private List<PodModel> getVersionRemovedPods(List<PodResponse> allPods) {
        List<PodModel> versionRemovedPods = new ArrayList<>();

        allPods.stream()
                .filter(p -> p.getPodState().equals("REMOVED"))
                .forEach(p -> versionRemovedPods.add(new PodModel().from(p)));

        return versionRemovedPods;
    }

    private List<PodModel> getVersionAddedPods(List<PodResponse> allPods) {
        List<PodModel> versionAddedPods = new ArrayList<>();

        allPods.stream()
                .filter(p -> p.getPodState().equals("ADDED"))
                .forEach(p -> versionAddedPods.add(new PodModel().from(p)));

        return versionAddedPods;
    }

    private List<PodModel> getVersionPods(List<PodResponse> allPods) {
        List<PodModel> versionPods = new ArrayList<>();

        allPods.stream()
                .filter(p -> p.getPodState().equals("CURRENT"))
                .forEach(p -> versionPods.add(new PodModel().from(p)));

        return versionPods;
    }

    private List<InterimAdvancePaymentDetailModel> getInterimAdvancePaymentDetailModels(Long id, Integer versionId) {
        List<InterimAdvancePaymentDetailResponse> interimAdvancePaymentDetailResponses = productContractInterimAdvancePaymentsRepository.fetchInterimAdvancePaymentsForDocument(id, versionId);
        List<InterimAdvancePaymentDetailModel> interims = new ArrayList<>();
        interimAdvancePaymentDetailResponses.forEach(i -> interims.add(new InterimAdvancePaymentDetailModel().from(i)));
        return interims;
    }

    private List<ManagerModel> getManagerModels(Long id, Integer versionId) {
        List<ManagersResponse> managersResponses = managerRepository.fetchManagersForProductContractDocument(id, versionId);
        List<ManagerProxyResponse> managerProxyResponses = proxyManagersRepository.fetchManagerProxiesForDocument(id, versionId);
        Map<Long, List<ManagerProxyResponse>> managerProxies = managerProxyResponses.stream().collect(Collectors.groupingBy(ManagerProxyResponse::getManagerId));
        List<ManagerModel> managers = new ArrayList<>();
        for (ManagersResponse manager : managersResponses) {
            List<ManagerProxyModel> managerProxyModels = new ArrayList<>();
            List<ManagerProxyResponse> proxyResponses = managerProxies.get(manager.getId());
            if (proxyResponses != null) {
                proxyResponses.forEach(r -> managerProxyModels.add(new ManagerProxyModel().from(r)));
            }
            managers.add(new ManagerModel().from(manager, managerProxyModels));
        }
        return managers;
    }

    @Override
    protected String folderPath() {
        return null;
    }
}
