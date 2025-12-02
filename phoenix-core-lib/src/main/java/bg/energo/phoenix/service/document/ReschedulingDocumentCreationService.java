package bg.energo.phoenix.service.document;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.IllegalArgumentsProvidedException;
import bg.energo.phoenix.model.documentModels.ReschedulingDocumentModel;
import bg.energo.phoenix.model.documentModels.ReschedulingInstallmentModel;
import bg.energo.phoenix.model.documentModels.ReschedulingLiabilityModel;
import bg.energo.phoenix.model.documentModels.ReschedulingManagerModel;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.customer.Manager;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.entity.receivable.rescheduling.Rescheduling;
import bg.energo.phoenix.model.entity.receivable.rescheduling.ReschedulingSignableDocuments;
import bg.energo.phoenix.model.entity.template.ContractTemplateDetail;
import bg.energo.phoenix.model.enums.customer.CustomerCommContactTypes;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.enums.documents.DocumentSigners;
import bg.energo.phoenix.model.enums.documents.DocumentStatus;
import bg.energo.phoenix.model.enums.template.ContractTemplateFileName;
import bg.energo.phoenix.model.enums.template.ContractTemplatePurposes;
import bg.energo.phoenix.model.enums.template.ContractTemplateSigning;
import bg.energo.phoenix.model.enums.template.ContractTemplateStatus;
import bg.energo.phoenix.model.request.receivable.rescheduling.ReschedulingTemplateRequest;
import bg.energo.phoenix.model.response.customer.communicationData.detailed.ContactDetailedResponse;
import bg.energo.phoenix.model.response.customer.communicationData.detailed.CustomerCommunicationsDetailedResponse;
import bg.energo.phoenix.model.response.receivable.rescheduling.ReschedulingAddressResponse;
import bg.energo.phoenix.model.response.receivable.rescheduling.ReschedulingContractsResponse;
import bg.energo.phoenix.repository.billing.companyDetails.CompanyDetailRepository;
import bg.energo.phoenix.repository.billing.companyDetails.CompanyLogoRepository;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.customer.ManagerRepository;
import bg.energo.phoenix.repository.documents.DocumentsRepository;
import bg.energo.phoenix.repository.receivable.rescheduling.ReschedulingFilesRepository;
import bg.energo.phoenix.repository.receivable.rescheduling.ReschedulingLiabilitiesRepository;
import bg.energo.phoenix.repository.receivable.rescheduling.ReschedulingRepository;
import bg.energo.phoenix.repository.receivable.rescheduling.ReschedulingSignableDocumentsRepository;
import bg.energo.phoenix.repository.template.ContractTemplateDetailsRepository;
import bg.energo.phoenix.repository.template.ContractTemplateRepository;
import bg.energo.phoenix.service.customer.customerCommunications.CustomerCommunicationsService;
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

import java.util.*;
import java.util.stream.Collectors;

import static bg.energo.phoenix.exception.ErrorCode.APPLICATION_ERROR;

@Service
@Slf4j
public class ReschedulingDocumentCreationService extends AbstractDocumentCreationService {

    private final ReschedulingRepository reschedulingRepository;
    private final CustomerRepository customerRepository;
    private final CustomerDetailsRepository customerDetailsRepository;
    private final ManagerRepository managerRepository;
    private final CustomerCommunicationsService customerCommunicationsService;
    private final ReschedulingLiabilitiesRepository reschedulingLiabilitiesRepository;
    private final ReschedulingFilesRepository reschedulingFilesRepository;
    private final ReschedulingSignableDocumentsRepository reschedulingSignableDocumentsRepository;
    private static final String FOLDER_PATH = "rescheduling_document";

    public ReschedulingDocumentCreationService(
            ContractTemplateDetailsRepository contractTemplateDetailsRepository,
            ContractTemplateRepository contractTemplateRepository,
            DocumentGenerationService documentGenerationService,
            DocumentGenerationUtil documentGenerationUtil,
            DocumentsRepository documentsRepository,
            CompanyDetailRepository companyDetailRepository,
            CompanyLogoRepository companyLogoRepository,
            SignerChainManager signerChainManager,
            FileService fileService,
            ReschedulingRepository reschedulingRepository,
            CustomerRepository customerRepository,
            CustomerDetailsRepository customerDetailsRepository,
            ManagerRepository managerRepository,
            CustomerCommunicationsService customerCommunicationsService,
            ReschedulingLiabilitiesRepository reschedulingLiabilitiesRepository,
            ReschedulingFilesRepository reschedulingFilesRepository,
            ReschedulingSignableDocumentsRepository reschedulingSignableDocumentsRepository
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
        this.reschedulingRepository = reschedulingRepository;
        this.customerRepository = customerRepository;
        this.customerDetailsRepository = customerDetailsRepository;
        this.managerRepository = managerRepository;
        this.customerCommunicationsService = customerCommunicationsService;
        this.reschedulingLiabilitiesRepository = reschedulingLiabilitiesRepository;
        this.reschedulingFilesRepository = reschedulingFilesRepository;
        this.reschedulingSignableDocumentsRepository = reschedulingSignableDocumentsRepository;
    }

    /**
     * Generates rescheduling documents based on the provided template requests.
     *
     * @param reschedulingId       the ID of the rescheduling to generate documents for
     * @param templateRequests     a list of template requests containing the details for each document to generate
     * @throws DomainEntityNotFoundException if the rescheduling, customer, or customer details cannot be found
     * @throws IllegalArgumentsProvidedException if any of the provided template IDs are invalid or do not match the expected file formats and signing options
     * @throws ClientException if an exception occurs while generating the documents
     */
    @Transactional
    public void generateDocument(Long reschedulingId, List<ReschedulingTemplateRequest> templateRequests) {
        ReschedulingDocumentModel reschedulingDocumentModel = buildModelWithSharedInfo(new ReschedulingDocumentModel());

        Rescheduling rescheduling = reschedulingRepository
                .findByIdAndStatus(reschedulingId, EntityStatus.ACTIVE)
                .orElseThrow(() -> new DomainEntityNotFoundException("Rescheduling not found by ID: %s;".formatted(reschedulingId)));

        Customer customer = customerRepository
                .findById(rescheduling.getCustomerId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Customer not found by ID: %s".formatted(rescheduling.getCustomerId())));

        CustomerDetails customerDetails = customerDetailsRepository
                .findById(rescheduling.getCustomerDetailId() != null ? rescheduling.getCustomerDetailId() : customer.getLastCustomerDetailId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Customer Details not found by ID: %s".formatted(customer.getLastCustomerDetailId())));

        mapReschedulingJsonTemplate(rescheduling, reschedulingDocumentModel, customerDetails);

        List<Long> templateIds = templateRequests.stream()
                .map(ReschedulingTemplateRequest::getTemplateId)
                .toList();

        Set<Long> validTemplateIds = contractTemplateRepository.findAllIdByIdAndStatusAndPurpose(
                new HashSet<>(templateIds),
                ContractTemplatePurposes.RESCHEDULING,
                ContractTemplateStatus.ACTIVE);

        StringBuilder errors = new StringBuilder();
        for (int i = 0; i < templateRequests.size(); i++) {
            ReschedulingTemplateRequest request = templateRequests.get(i);
            if (!validTemplateIds.contains(request.getTemplateId())) {
                errors.append("documents[%s].templateId-Template with id %s was not found or has wrong purpose!;".formatted(i, request.getTemplateId()));
            }
        }

        if (!errors.isEmpty()) {
            throw new IllegalArgumentsProvidedException(errors.toString());
        }

        Set<ContractTemplateDetail> templateDetails = contractTemplateDetailsRepository.findRespectiveTemplateDetailsByTemplateIds(validTemplateIds);
        Map<Long, ContractTemplateDetail> templateDetailMap = templateDetails
                .stream()
                .collect(Collectors.toMap(ContractTemplateDetail::getTemplateId, detail -> detail));

        for (int i = 0; i < templateRequests.size(); i++) {
            ReschedulingTemplateRequest request = templateRequests.get(i);
            ContractTemplateDetail template = templateDetailMap.get(request.getTemplateId());

            if (!template.getOutputFileFormat().stream()
                    .map(FileFormat::fromContractTemplateFileFormat)
                    .collect(Collectors.toSet())
                    .containsAll(request.getOutputFileFormat())) {
                errors.append("documents[%s].outputFileFormat-Template with id %s does not contain all provided output file formats!;".formatted(i, request.getTemplateId()));
            }

            if (!template.getOutputFileFormat().stream()
                    .map(FileFormat::fromContractTemplateFileFormat)
                    .collect(Collectors.toSet())
                    .containsAll(request.getOutputFileFormat())) {
                errors.append("documents[%s].signings-Template with id %s does not contain all provided signing options!;".formatted(i, request.getTemplateId()));
            }
        }

        if (!errors.isEmpty()) {
            throw new IllegalArgumentsProvidedException(errors.toString());
        }

        List<Document> documentList = new ArrayList<>();

        for (ReschedulingTemplateRequest templateRequest : templateRequests) {
            ContractTemplateDetail template = templateDetailMap.get(templateRequest.getTemplateId());

            try {
                ByteArrayResource templateFileResource = createTemplateFileResource(template);

                String fileName = formatDocumentFileName(rescheduling, template, customerDetails.getId());

                log.debug("Generating document for template: {}", template.getTemplateId());
                DocumentPathPayloads documentPathPayloads = documentGenerationService.generateDocument(
                        templateFileResource,
                        buildFileDestinationPath(),
                        fileName,
                        reschedulingDocumentModel,
                        new HashSet<>(templateRequest.getOutputFileFormat()),
                        false
                );
                log.debug("Documents created successfully");

                for (FileFormat format : templateRequest.getOutputFileFormat()) {
                    String generatedDocumentPath = documentGenerationUtil.getGeneratedDocumentPath(format, documentPathPayloads);
                    Document savedDocument = saveDocument(
                            String.format("%s.%s", fileName, format.suffix),
                            generatedDocumentPath,
                            format,
                            getDocumentSigners(templateRequest),
                            rescheduling.getId(),
                            templateRequest.getTemplateId()
                    );
                    if (FileFormat.PDF.equals(format)) {
                        documentList.add(savedDocument);
                    }
                }

            } catch (Exception e) {
                log.error("Exception while generating document for template {}: {}", template.getTemplateId(), e.getMessage());
                throw new ClientException("Exception while generating document: %s".formatted(e.getMessage()), APPLICATION_ERROR);
            }
        }

        signerChainManager.startSign(documentList);
    }

    /**
     * Retrieves the list of document signers for the given rescheduling template request.
     *
     * @param request the rescheduling template request
     * @return the list of document signers
     */
    private List<DocumentSigners> getDocumentSigners(ReschedulingTemplateRequest request) {
        return Optional
                .ofNullable(request.getSignings())
                .orElse(Collections.emptyList())
                .stream()
                .map(ContractTemplateSigning::getDocumentSigners)
                .toList();
    }

    /**
     * Saves a document to the database and associates it with a rescheduling.
     *
     * @param fileName         the name of the document file
     * @param ftpPath          the path to the document file on the FTP server
     * @param fileFormat       the format of the document file
     * @param documentSigners  the list of document signers
     * @param reschedulingId    the ID of the rescheduling the document is associated with
     * @param templateId        the ID of the template used to generate the document
     * @return the saved document
     */
    private Document saveDocument(
            String fileName,
            String ftpPath,
            FileFormat fileFormat,
            List<DocumentSigners> documentSigners,
            Long reschedulingId,
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
                .documentStatus(FileFormat.PDF.equals(fileFormat) ? DocumentStatus.UNSIGNED : DocumentStatus.SIGNED)
                .status(EntityStatus.ACTIVE)
                .build();

        Document savedDoc = documentsRepository.saveAndFlush(document);

        reschedulingSignableDocumentsRepository.save(new ReschedulingSignableDocuments(null, savedDoc.getId(), reschedulingId, EntityStatus.ACTIVE));

        return savedDoc;
    }


    /**
     * Maps the rescheduling data to a ReschedulingDocumentModel.
     * This method retrieves the customer address information, managers, contracts, liabilities, and installments associated with the rescheduling, and populates the ReschedulingDocumentModel with this data.
     *
     * @param rescheduling the Rescheduling object containing the rescheduling data
     * @param reschedulingDocumentModel the ReschedulingDocumentModel to be populated with the rescheduling data
     * @param customerDetails the CustomerDetails object containing the customer information
     */
    private void mapReschedulingJsonTemplate(Rescheduling rescheduling, ReschedulingDocumentModel reschedulingDocumentModel, CustomerDetails customerDetails) {
        ReschedulingAddressResponse addressInfo = reschedulingRepository
                .findCustomerAddressInfoForRescheduling(rescheduling.getId(), customerDetails.getId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Customer address info not found;"));

        List<Manager> managers = managerRepository.findManagersByCustomerDetailId(
                customerDetails.getId(),
                List.of(Status.ACTIVE)
        );

        Optional<ReschedulingContractsResponse> contractsByReschedulingId = reschedulingLiabilitiesRepository.getContractsByReschedulingId(rescheduling.getId());
        StringBuilder contracts = new StringBuilder();
        if (contractsByReschedulingId.isPresent()) {
            if (contractsByReschedulingId.get().getProductContractNumbers() != null) {
                contracts.append(contractsByReschedulingId.get().getProductContractNumbers());
                if (contractsByReschedulingId.get().getServiceContractNumbers() != null) {
                    contracts.append(",");
                }
            }
            if (contractsByReschedulingId.get().getServiceContractNumbers() != null) {
                contracts.append(contractsByReschedulingId.get().getServiceContractNumbers());
            }
        }
        String email = getCustomerEmail(customerDetails.getId());
        List<ReschedulingLiabilityModel> reschedulingLiabilitiesByReschedulingId = reschedulingRepository
                .findReschedulingLiabilitiesByReschedulingId(rescheduling.getId())
                .stream()
                .map(object -> {
                            ReschedulingLiabilityModel reschedulingLiabilityModel = new ReschedulingLiabilityModel();
                            reschedulingLiabilityModel.from(object);
                            return reschedulingLiabilityModel;
                        }
                )
                .toList();

        List<ReschedulingInstallmentModel> installmentsByReschedulingId = reschedulingRepository
                .findInstallmentsByReschedulingId(rescheduling.getId())
                .stream()
                .map(plan -> {
                            ReschedulingInstallmentModel reschedulingInstallmentModel = new ReschedulingInstallmentModel();
                            reschedulingInstallmentModel.from(plan);
                            return reschedulingInstallmentModel;
                        }
                )
                .toList();

        reschedulingDocumentModel.from(
                addressInfo,
                managers
                        .stream()
                        .map(manager ->
                                new ReschedulingManagerModel(
                                        manager.getTitle().getName(),
                                        manager.getName(),
                                        manager.getSurname(),
                                        manager.getJobPosition()
                                )
                        )
                        .collect(Collectors.toList()),
                rescheduling,
                installmentsByReschedulingId,
                reschedulingLiabilitiesByReschedulingId,
                email,
                contracts.toString()
        );
    }

    /**
     * Retrieves the email address for the customer with the given customer detail ID.
     *
     * @param customerDetailId the ID of the customer detail for which to retrieve the email address
     * @return the email address for the customer, or null if no email address is found
     */
    private String getCustomerEmail(Long customerDetailId) {
        List<CustomerCommunicationsDetailedResponse> communications = customerCommunicationsService.getCustomerCommunicationsByCustomerDetailId(customerDetailId);

        if (communications == null || communications.isEmpty()) {
            return null;
        }

        return communications
                .stream()
                .flatMap(comm -> comm.getCommunicationContacts().stream())
                .filter(contact -> CustomerCommContactTypes.EMAIL.equals(contact.getContactType()))
                .max(Comparator.comparing(ContactDetailedResponse::getId))
                .map(ContactDetailedResponse::getContactValue)
                .orElse(null);
    }

    /**
     * Formats the document file name based on the provided Rescheduling, ContractTemplateDetail, and customer detail ID.
     * The file name is constructed by combining the file prefix, file name, and file suffix, with the following rules:
     * - The file prefix is extracted using the `documentGenerationUtil.extractFilePrefix()` method.
     * - The file name is extracted using the `extractFileName()` method, which generates the file name based on the customer information.
     * - The file suffix is extracted using the `extractFileSuffix()` method, which formats the file name suffix based on the Rescheduling's create date.
     * - Any blank parts are removed from the file name parts.
     * - The final file name is constructed by joining the parts with an underscore, and is limited to a maximum of 200 characters.
     * - The file name is then appended with the ".pdf" extension, and any forward slashes are replaced with underscores.
     *
     * @param rescheduling the Rescheduling object used to extract the file name parts
     * @param contractTemplateDetail the ContractTemplateDetail object used to extract the file name parts
     * @param customerDetailId the customer detail ID used to extract the file name parts
     * @return the formatted document file name
     */
    private String formatDocumentFileName(Rescheduling rescheduling, ContractTemplateDetail contractTemplateDetail, Long customerDetailId) {
        log.debug("Starting file name formatting");
        List<String> fileNameParts = new ArrayList<>();
        log.debug("Extracting file prefix");
        fileNameParts.add(documentGenerationUtil.extractFilePrefix(contractTemplateDetail));
        log.debug("Extracting file name");
        fileNameParts.add(extractFileName(rescheduling, contractTemplateDetail, customerDetailId));
        log.debug("Extracting file suffix");
        fileNameParts.add(extractFileSuffix(rescheduling.getCreateDate(), contractTemplateDetail));
        fileNameParts.removeIf(StringUtils::isBlank);

        String fileParts = String.join("_", fileNameParts);
        fileParts = StringUtils.isBlank(fileParts) ? "RESCHEDULING" : fileParts;
        return fileParts.substring(0, Math.min(fileParts.length(), 200)).replaceAll("/", "_");
    }

    /**
     * Extracts the file name for a Rescheduling document based on the provided ContractTemplateDetail and customer details.
     * The file name is constructed by combining various parts, such as the customer identifier, customer name, customer number, document number, file ID, and timestamp.
     * The order and format of the file name parts are determined by the ContractTemplateFileName entries in the ContractTemplateDetail.
     * If the ContractTemplateFileName list is empty, the Rescheduling number is used as the file name.
     *
     * @param rescheduling the Rescheduling object used to extract the file name parts
     * @param contractTemplateDetail the ContractTemplateDetail object used to extract the file name parts
     * @param customerDetailId the customer detail ID used to extract the file name parts
     * @return the formatted file name
     */
    private String extractFileName(Rescheduling rescheduling, ContractTemplateDetail contractTemplateDetail, Long customerDetailId) {
        try {
            List<ContractTemplateFileName> fileName = contractTemplateDetail
                    .getFileName()
                    .stream()
                    .sorted(Comparator.comparing(ContractTemplateFileName::getPriority))
                    .toList();

            if (org.springframework.util.CollectionUtils.isEmpty(fileName)) {
                return rescheduling.getReschedulingNumber();
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
                            Pair<Customer, CustomerDetails> customerPair = documentGenerationUtil.getCustomer(customerDetailId, customerCache);
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
                            Pair<Customer, CustomerDetails> customerPair = documentGenerationUtil.getCustomer(customerDetailId, customerCache);
                            Customer customer = customerPair.getKey();
                            nameParts.add(String.valueOf(customer.getCustomerNumber()));
                        }
                        case DOCUMENT_NUMBER -> nameParts.add(rescheduling.getReschedulingNumber());
                        case FILE_ID -> nameParts.add(String.valueOf(reschedulingFilesRepository.getNextIdValue()));
                        case TIMESTAMP -> nameParts.add(rescheduling.getCreateDate().toString());
                    }
                }

                return String.join("_", nameParts);
            }
        } catch (Exception e) {
            log.error("Exception handled while trying to extract file name", e);
            return rescheduling.getReschedulingNumber();
        }
    }

    @Override
    protected String folderPath() {
        return FOLDER_PATH;
    }
}
