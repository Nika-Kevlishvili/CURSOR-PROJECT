package bg.energo.phoenix.service.contract.service;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.exception.IllegalArgumentsProvidedException;
import bg.energo.phoenix.model.documentModels.contract.*;
import bg.energo.phoenix.model.documentModels.contract.response.*;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.service.ServiceContractDetails;
import bg.energo.phoenix.model.entity.contract.service.ServiceContractSignableDocuments;
import bg.energo.phoenix.model.entity.contract.service.ServiceContracts;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.entity.template.ContractTemplateDetail;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailsStatus;
import bg.energo.phoenix.model.enums.contract.service.ServiceContractDetailStatus;
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
import bg.energo.phoenix.repository.contract.service.*;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.customer.ManagerRepository;
import bg.energo.phoenix.repository.documents.DocumentsRepository;
import bg.energo.phoenix.repository.product.service.subObject.ServicePriceComponentRepository;
import bg.energo.phoenix.repository.template.ContractTemplateDetailsRepository;
import bg.energo.phoenix.repository.template.ContractTemplateRepository;
import bg.energo.phoenix.repository.template.ServiceTemplateRepository;
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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static bg.energo.phoenix.exception.ErrorCode.APPLICATION_ERROR;

@Service
@Slf4j
public class ServiceContractDocumentCreationService extends AbstractDocumentCreationService {

    private final ServiceContractsRepository serviceContractsRepository;
    private final ManagerRepository managerRepository;
    private final ServiceContractProxyManagersRepository proxyManagersRepository;
    private final ServiceContractInterimAdvancePaymentsRepository serviceContractInterimAdvancePaymentsRepository;
    private final ServicePriceComponentRepository servicePriceComponentRepository;
    private final ServiceTemplateRepository serviceTemplateRepository;
    private final ServiceContractDetailsRepository serviceContractDetailsRepository;
    private final CustomerRepository customerRepository;
    private final CustomerDetailsRepository customerDetailsRepository;
    private final ServiceContractFilesRepository serviceContractFilesRepository;
    private final PermissionService permissionService;
    private final ServiceContractSignableDocumentsRepository signableDocumentsRepository;
    private final EDMSAttributeProperties edmsAttributeProperties;
    private final FileArchivationService fileArchivationService;
    private final ContractDocumentTranslationUtil translationUtil;
    private final ServiceContractPodsRepository contractPodsRepository;

    private static String FOLDER_PATH = "service_contract_files";

    public ServiceContractDocumentCreationService(
            ContractTemplateDetailsRepository contractTemplateDetailsRepository,
            ContractTemplateRepository contractTemplateRepository,
            DocumentGenerationService documentGenerationService,
            DocumentGenerationUtil documentGenerationUtil,
            DocumentsRepository documentsRepository,
            CompanyDetailRepository companyDetailRepository,
            CompanyLogoRepository companyLogoRepository,
            SignerChainManager signerChainManager,
            FileService fileService,
            ServiceContractsRepository serviceContractsRepository,
            ManagerRepository managerRepository,
            ServiceContractProxyManagersRepository proxyManagersRepository,
            ServiceContractInterimAdvancePaymentsRepository serviceContractInterimAdvancePaymentsRepository,
            ServicePriceComponentRepository servicePriceComponentRepository,
            ServiceTemplateRepository serviceTemplateRepository,
            ServiceContractDetailsRepository serviceContractDetailsRepository,
            CustomerRepository customerRepository,
            CustomerDetailsRepository customerDetailsRepository,
            ServiceContractFilesRepository serviceContractFilesRepository,
            PermissionService permissionService,
            ServiceContractSignableDocumentsRepository signableDocumentsRepository,
            EDMSAttributeProperties edmsAttributeProperties,
            FileArchivationService fileArchivationService,
            ContractDocumentTranslationUtil translationUtil, ServiceContractPodsRepository contractPodsRepository
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
        this.serviceContractsRepository = serviceContractsRepository;
        this.managerRepository = managerRepository;
        this.proxyManagersRepository = proxyManagersRepository;
        this.serviceContractInterimAdvancePaymentsRepository = serviceContractInterimAdvancePaymentsRepository;
        this.servicePriceComponentRepository = servicePriceComponentRepository;
        this.serviceTemplateRepository = serviceTemplateRepository;
        this.serviceContractDetailsRepository = serviceContractDetailsRepository;
        this.customerRepository = customerRepository;
        this.customerDetailsRepository = customerDetailsRepository;
        this.serviceContractFilesRepository = serviceContractFilesRepository;
        this.permissionService = permissionService;
        this.signableDocumentsRepository = signableDocumentsRepository;
        this.edmsAttributeProperties = edmsAttributeProperties;
        this.fileArchivationService = fileArchivationService;
        this.translationUtil = translationUtil;
        this.contractPodsRepository = contractPodsRepository;
    }

    /**
     * Retrieves the list of document generation popup responses for the specified contract and version.
     *
     * @param id        The ID of the contract.
     * @param versionId The ID of the contract version.
     * @return A list of {@link DocumentGenerationPopupResponse} objects containing the details of the available document templates.
     * @throws ClientException               if the user does not have the necessary permissions to generate documents for the contract.
     * @throws DomainEntityNotFoundException if the contract or contract version cannot be found, or if the contract has an invalid status for document generation.
     */
    public List<DocumentGenerationPopupResponse> getDocumentGenerationPopup(Long id, Long versionId) {
        if (!permissionService.permissionContextContainsPermissions(PermissionContextEnum.SERVICE_CONTRACTS, List.of(PermissionEnum.SERVICE_CONTRACT_GENERATE))) {
            throw new ClientException("You can not generate documents for this contract!;", ErrorCode.ACCESS_DENIED);
        }

        if (!serviceContractsRepository.existsByIdAndStatusAndContractStatusNotIn(id, EntityStatus.ACTIVE, List.of(ServiceContractDetailStatus.TERMINATED, ServiceContractDetailStatus.CANCELLED))) {
            throw new DomainEntityNotFoundException("id-Contract can't be found or has wrong status for generation;");
        }

        if (!serviceContractDetailsRepository.existsByContractIdAndVersionId(id, versionId)) {
            throw new DomainEntityNotFoundException("versionId-version not found!;");
        }

        return serviceTemplateRepository.findTemplatesForContractDocumentGeneration(id, versionId, getTemplatePurposes())
                .stream()
                .map(response -> new DocumentGenerationPopupResponse(
                                response.getTemplateId(),
                                response.getTemplateVersion(), response.getTemplateName(),
                                response.getOutputFileFormat(), response.getFileSignings()
                        )
                )
                .toList();
    }


    /**
     * Generates documents for the specified contract and version based on the provided request.
     *
     * @param request The contract document save request containing the details of the documents to be generated.
     * @throws DomainEntityNotFoundException     if the contract or contract version cannot be found, or if the contract has an invalid status for document generation.
     * @throws IllegalArgumentsProvidedException if the provided document templates or output file formats are not valid for the contract.
     */
    @Transactional
    public void generateDocument(ContractDocumentSaveRequest request) {
        Long id = request.getContractId();
        Long versionId = request.getVersionId();
        ServiceContracts serviceContracts = serviceContractsRepository
                .findByIdAndStatusAndContractStatusIsNotIn(id, EntityStatus.ACTIVE, List.of(ServiceContractDetailStatus.CANCELLED, ServiceContractDetailStatus.TERMINATED))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Service contract not found or has invalid status for document generation!"));

        ServiceContractDetails details = serviceContractDetailsRepository
                .findByContractIdAndVersionId(id, versionId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Service contract details not found"));

        ContractDocumentModel contractDocumentModel = fetchDocumentModel(id, versionId);

        List<ContractDocumentSaveRequest.DocumentSaveRequestModels> documents = request.getDocuments();
        List<Long> list = documents.stream().map(ContractDocumentSaveRequest.DocumentSaveRequestModels::getTemplateId).toList();
        Set<Long> fetchedTemplateIds = serviceTemplateRepository.findForContractDocument(id, versionId, list, getTemplatePurposes());
        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i < documents.size(); i++) {
            Long templateId = documents.get(i).getTemplateId();
            if (!fetchedTemplateIds.contains(templateId)) {
                stringBuilder.append("documents[%s].templateId-Service does not have provided template with id %s!;".formatted(i, templateId));
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

        generateDocument(documents, templateDetailMap, serviceContracts, details, contractDocumentModel);
    }

    private void generateDocument(List<ContractDocumentSaveRequest.DocumentSaveRequestModels> documents, Map<Long, ContractTemplateDetail> templateDetailMap, ServiceContracts serviceContracts, ServiceContractDetails serviceContractDetails, ContractDocumentModel contractDocumentModel) {
        log.debug("deleting previous files");
        deletePreviousFiles(documents, serviceContractDetails);

        List<Document> documentList = new ArrayList<>();

        for (ContractDocumentSaveRequest.DocumentSaveRequestModels document : documents) {

            ContractTemplateDetail template = templateDetailMap.get(document.getTemplateId());
            String destinationPath = buildFileDestinationPath();

            ByteArrayResource templateFileResource;
            try {
                templateFileResource = createTemplateFileResource(template);
                log.debug("Clone of template file created");

                String fileName = formatFileName(serviceContracts, serviceContractDetails, template);
                log.debug("File name was formatted: [%s]".formatted(fileName));

                log.debug("Generating document");
                DocumentPathPayloads documentPathPayloads = documentGenerationService.generateDocument(
                        templateFileResource,
                        destinationPath,
                        fileName,
                        contractDocumentModel,
                        new HashSet<>(document.getOutputFileFormat()),
                        serviceContracts.getContractStatus() == ServiceContractDetailStatus.DRAFT
                );
                log.debug("Documents was created successfully");

                log.debug("Saving service contract document entity");

                for (FileFormat format : document.getOutputFileFormat()) {
                    String generatedDocumentPath = documentGenerationUtil.getGeneratedDocumentPath(format, documentPathPayloads);
                    saveDocuments(
                            String.format("%s.%s", fileName, format.suffix),
                            generatedDocumentPath,
                            format,
                            getDocumentSigners(document),
                            serviceContractDetails.getId(),
                            document.getTemplateId(),
                            documentList,
                            serviceContracts.getContractNumber()
                    );
                }
            } catch (Exception e) {
                log.error("exception while storing template file: {}", e.getMessage());
                throw new ClientException("exception while storing template file: %s".formatted(e.getMessage()), APPLICATION_ERROR);
            }
        }

        List<ServiceContractDetailStatus> validStatusesForSigning = List.of(
                ServiceContractDetailStatus.READY,
                ServiceContractDetailStatus.SIGNED,
                ServiceContractDetailStatus.ENTERED_INTO_FORCE,
                ServiceContractDetailStatus.ACTIVE_IN_TERM,
                ServiceContractDetailStatus.ACTIVE_IN_PERPETUITY,
                ServiceContractDetailStatus.CHANGED_WITH_AGREEMENT
        );

        if (validStatusesForSigning.contains(serviceContracts.getContractStatus())) {
            signerChainManager.startSign(documentList);
        }
    }

    private List<DocumentSigners> getDocumentSigners(ContractDocumentSaveRequest.DocumentSaveRequestModels document) {
        return Optional
                .ofNullable(document.getSignings())
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
                .signers(documentSigners)
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
        signableDocumentsRepository.save(new ServiceContractSignableDocuments(null, document.getId(), contractDetailId, false, EntityStatus.ACTIVE));
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

    private void deletePreviousFiles(List<ContractDocumentSaveRequest.DocumentSaveRequestModels> documents, ServiceContractDetails serviceContractDetails) {
        List<Long> templateIds = documents.stream().filter(ContractDocumentSaveRequest.DocumentSaveRequestModels::isDeletePreviousFiles).map(ContractDocumentSaveRequest.DocumentSaveRequestModels::getTemplateId).toList();
        List<Document> documentPreviousFiles = signableDocumentsRepository.getDocumentPreviousFiles(serviceContractDetails.getId(), templateIds);
        documentPreviousFiles.forEach(document -> document.setStatus(EntityStatus.DELETED));
        documentsRepository.saveAllAndFlush(documentPreviousFiles);
    }

    private String formatFileName(ServiceContracts contract, ServiceContractDetails contractDetails, ContractTemplateDetail contractTemplateDetail) {
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

    private String extractFileName(ServiceContracts contract, ServiceContractDetails contractDetails, ContractTemplateDetail contractTemplateDetail) {
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
                        case FILE_ID -> nameParts.add(String.valueOf(serviceContractFilesRepository.getNextIdValue()));
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

            CustomerDetails customerDetails = customerDetailsRepository.findById(customerDetailId)
                    .orElseThrow(() -> new DomainEntityNotFoundException("Customer with detail id: [%s] not found;".formatted(customerDetailId)));

            customerCache.put(customerDetailId, Pair.of(customer, customerDetails));
        }

        return Pair.of(customerCache.get(customerDetailId).getKey(), customerCache.get(customerDetailId).getValue());
    }

    public ContractDocumentModel fetchDocumentModel(Long id, Long versionId) {
        ContractMainResponse contractMainResponse = serviceContractsRepository.fetchContractInfoForDocument(id, versionId);

        List<ManagerModel> managers = getManagerModels(id, versionId);

        List<InterimAdvancePaymentDetailModel> interims = getInterimAdvancePaymentDetailModels(id, versionId);

        List<PriceComponentModel> priceComponentModels = getPriceComponentModels(id, versionId);

        List<PodResponse> allPods = contractPodsRepository.fetchVersionPodsForDocument(id, versionId);
        List<PodModel> versionAddedPods = getVersionAddedPods(allPods);
        List<PodModel> versionPods = getVersionPods(allPods);
        List<PodModel> versionRemovedPods = getVersionRemovedPods(allPods);

        translationUtil.translatePods(versionAddedPods);
        translationUtil.translatePods(versionPods);
        translationUtil.translatePods(versionRemovedPods);

        ContractDocumentModel documentModel = new ContractDocumentModel().from(contractMainResponse, managers, interims, versionPods, versionAddedPods, versionRemovedPods, priceComponentModels);
        translationUtil.translateModel(documentModel);
        return buildModelWithSharedInfo(documentModel);
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

    public Optional<ByteArrayResource> generateEmailDocument(Long id, Long versionId, Long emailTemplateId) {
        try {

            ContractDocumentModel contractDocumentModel = fetchDocumentModel(id, versionId);
            LocalDate currentDate = LocalDate.now();
            Optional<ContractTemplateDetail> templateDetailOptional = contractTemplateDetailsRepository
                    .findRespectiveTemplateDetailsByTemplateIdAndDate(emailTemplateId, currentDate);

            if (templateDetailOptional.isEmpty()) {
                log.error("Template detail not found for Email Template ID: [{}], Date: [{}]", emailTemplateId, currentDate);
                return Optional.empty();
            }

            ContractTemplateDetail contractTemplateDetail = templateDetailOptional.get();
            String destinationPath = "%s/%s/%s".formatted(ftpBasePath, "service_contract_files/email_documents", currentDate);

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


    private List<PriceComponentModel> getPriceComponentModels(Long id, Long versionId) {
        List<PriceComponentModel> priceComponentModels = new ArrayList<>();
        List<PriceComponentResponse> priceComponentResponses = servicePriceComponentRepository.fetchPriceComponentsForDocument(id, versionId);
        priceComponentResponses.forEach(pc -> priceComponentModels.add(new PriceComponentModel().from(pc)));
        return priceComponentModels;
    }


    private List<InterimAdvancePaymentDetailModel> getInterimAdvancePaymentDetailModels(Long id, Long versionId) {
        List<InterimAdvancePaymentDetailResponse> interimAdvancePaymentDetailResponses = serviceContractInterimAdvancePaymentsRepository.fetchInterimAdvancePaymentsForDocument(id, versionId);
        List<InterimAdvancePaymentDetailModel> interims = new ArrayList<>();
        interimAdvancePaymentDetailResponses.forEach(i -> interims.add(new InterimAdvancePaymentDetailModel().from(i)));
        return interims;
    }

    private List<ManagerModel> getManagerModels(Long id, Long versionId) {
        List<ManagersResponse> managersResponses = managerRepository.fetchManagersForServiceContractDocument(id, versionId);
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

    private List<String> getTemplatePurposes() {
        List<String> purposes = new ArrayList<>();
        if (permissionService.permissionContextContainsPermissions(PermissionContextEnum.SERVICE_CONTRACTS, List.of(PermissionEnum.SERVICE_CONTRACT_GENERATE_ADDITIONAL_AGREEMENT_BASE))) {
            purposes.add(ContractTemplatePurposes.SERVICE_ADDITIONAL_TEMPLATE_BASE.name());
        }
        if (permissionService.permissionContextContainsPermissions(PermissionContextEnum.SERVICE_CONTRACTS, List.of(PermissionEnum.SERVICE_CONTRACT_GENERATE_ADDITIONAL_AGREEMENT_ADVANCE))) {
            purposes.add(ContractTemplatePurposes.SERVICE_ADDITIONAL_TEMPLATE_ADVANCE.name());
        }
        return purposes;
    }

    @Override
    protected String folderPath() {
        return FOLDER_PATH;
    }
}

