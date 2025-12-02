package bg.energo.phoenix.service.document;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.documentModels.latePaymentFine.*;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.billing.ContractBillingGroup;
import bg.energo.phoenix.model.entity.crm.emailCommunication.EmailCommunicationAttachment;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.entity.nomenclature.crm.EmailMailboxes;
import bg.energo.phoenix.model.entity.nomenclature.crm.TopicOfCommunication;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.receivable.latePaymentFine.LatePaymentFine;
import bg.energo.phoenix.model.entity.receivable.latePaymentFine.LatePaymentFineCommunications;
import bg.energo.phoenix.model.entity.receivable.latePaymentFine.LatePaymentFineDocumentFile;
import bg.energo.phoenix.model.entity.template.ContractTemplate;
import bg.energo.phoenix.model.entity.template.ContractTemplateDetail;
import bg.energo.phoenix.model.enums.customer.CustomerCommContactTypes;
import bg.energo.phoenix.model.enums.customer.CustomerType;
import bg.energo.phoenix.model.enums.documents.DocumentSigners;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.receivable.latePaymentFine.LatePaymentFineType;
import bg.energo.phoenix.model.enums.template.ContractTemplateFileName;
import bg.energo.phoenix.model.enums.template.ContractTemplatePurposes;
import bg.energo.phoenix.model.request.crm.emailCommunication.DocumentEmailCommunicationCreateRequest;
import bg.energo.phoenix.repository.billing.companyDetails.CompanyDetailRepository;
import bg.energo.phoenix.repository.billing.companyDetails.CompanyLogoRepository;
import bg.energo.phoenix.repository.contract.billing.ContractBillingGroupRepository;
import bg.energo.phoenix.repository.crm.emailCommunication.EmailCommunicationAttachmentRepository;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommunicationsRepository;
import bg.energo.phoenix.repository.documents.DocumentsRepository;
import bg.energo.phoenix.repository.nomenclature.crm.EmailMailboxesRepository;
import bg.energo.phoenix.repository.nomenclature.crm.TopicOfCommunicationRepository;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.receivable.latePaymentFine.LatePaymentFineCommunicationsRepository;
import bg.energo.phoenix.repository.receivable.latePaymentFine.LatePaymentFineDocumentFileRepository;
import bg.energo.phoenix.repository.receivable.latePaymentFine.LatePaymentFineRepository;
import bg.energo.phoenix.repository.template.ContractTemplateDetailsRepository;
import bg.energo.phoenix.repository.template.ContractTemplateRepository;
import bg.energo.phoenix.service.crm.emailCommunication.EmailCommunicationService;
import bg.energo.phoenix.service.document.enums.DocumentPathPayloads;
import bg.energo.phoenix.service.document.enums.FileFormat;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.service.signing.SignerChainManager;
import bg.energo.phoenix.util.contract.CommunicationContactPurposeProperties;
import bg.energo.phoenix.util.template.document.DocumentGenerationUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static bg.energo.phoenix.exception.ErrorCode.APPLICATION_ERROR;
import static bg.energo.phoenix.service.document.DocumentParserService.parseDocxToHtml;

@Slf4j
@Service
public class LatePaymentFineDocumentCreationService extends AbstractDocumentCreationService {

    private static final String FOLDER_PATH = "late_payment_fine_document";
    private final LatePaymentFineRepository latePaymentFineRepository;
    private final CurrencyRepository currencyRepository;
    private final ContractBillingGroupRepository contractBillingGroupRepository;
    private final LatePaymentFineDocumentFileRepository latePaymentFineDocumentFileRepository;
    private final EmailCommunicationAttachmentRepository emailCommunicationAttachmentRepository;
    private final LatePaymentFineCommunicationsRepository latePaymentFineCommunicationsRepository;
    private final TopicOfCommunicationRepository topicOfCommunicationRepository;
    private final EmailMailboxesRepository emailMailboxesRepository;
    private final CustomerRepository customerRepository;
    private final CustomerDetailsRepository customerDetailsRepository;
    private final CustomerCommunicationsRepository customerCommunicationsRepository;
    private final EmailCommunicationService emailCommunicationService;
    private final CommunicationContactPurposeProperties communicationContactPurposeProperties;

    public LatePaymentFineDocumentCreationService(
            ContractTemplateDetailsRepository contractTemplateDetailsRepository,
            ContractTemplateRepository contractTemplateRepository,
            DocumentGenerationService documentGenerationService,
            DocumentGenerationUtil documentGenerationUtil,
            DocumentsRepository documentsRepository,
            CompanyDetailRepository companyDetailRepository,
            CompanyLogoRepository companyLogoRepository,
            SignerChainManager signerChainManager,
            FileService fileService,
            LatePaymentFineRepository latePaymentFineRepository,
            CurrencyRepository currencyRepository,
            ContractBillingGroupRepository contractBillingGroupRepository,
            LatePaymentFineDocumentFileRepository latePaymentFineDocumentFileRepository,
            EmailCommunicationAttachmentRepository emailCommunicationAttachmentRepository,
            LatePaymentFineCommunicationsRepository latePaymentFineCommunicationsRepository,
            TopicOfCommunicationRepository topicOfCommunicationRepository,
            EmailMailboxesRepository emailMailboxesRepository,
            CustomerRepository customerRepository,
            CustomerDetailsRepository customerDetailsRepository,
            CustomerCommunicationsRepository customerCommunicationsRepository,
            EmailCommunicationService emailCommunicationService,
            CommunicationContactPurposeProperties communicationContactPurposeProperties
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
        this.latePaymentFineRepository = latePaymentFineRepository;
        this.currencyRepository = currencyRepository;
        this.contractBillingGroupRepository = contractBillingGroupRepository;
        this.latePaymentFineDocumentFileRepository = latePaymentFineDocumentFileRepository;
        this.emailCommunicationAttachmentRepository = emailCommunicationAttachmentRepository;
        this.latePaymentFineCommunicationsRepository = latePaymentFineCommunicationsRepository;
        this.topicOfCommunicationRepository = topicOfCommunicationRepository;
        this.emailMailboxesRepository = emailMailboxesRepository;
        this.customerRepository = customerRepository;
        this.customerDetailsRepository = customerDetailsRepository;
        this.customerCommunicationsRepository = customerCommunicationsRepository;
        this.emailCommunicationService = emailCommunicationService;
        this.communicationContactPurposeProperties = communicationContactPurposeProperties;
    }

    /**
     * Generates a document related to a late payment fine and sends an email to the respective customer.
     * <p>
     * This method retrieves the relevant information about a late payment fine and its associated customer
     * and prepares a document and email notification. The document and email are generated based on predefined
     * templates and are sent to the customer using their registered communication data.
     *
     * @param latePaymentFineId the identifier of the late payment fine for which the document and email
     *                          need to be generated and sent
     * @throws DomainEntityNotFoundException if the late payment fine or customer information cannot be found
     * @throws ClientException               if there is any error during the document generation or email sending process
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void generateDocumentAndSendEmail(Long latePaymentFineId) {
        generateEmailAndDoc(latePaymentFineId);
    }

    /**
     * Generates a document and sends an email for a reversal associated with the given late payment fine ID.
     *
     * @param latePaymentFineId the ID of the late payment fine for which the document is to be generated and the email is to be sent
     * @return a list of IDs corresponding to the generated documents or emails
     */
    public List<Long> generateDocumentAndSendEmailForReversal(Long latePaymentFineId) {
        return generateEmailAndDoc(latePaymentFineId);
    }

    /**
     * Generates emails and documents based on the provided LatePaymentFine entity.
     * Creates email communications with generated documents attached, saves them in the system,
     * and returns the list of generated email IDs.
     *
     * @param latePaymentFineId The identifier of the LatePaymentFine entity for which the emails and documents are to be generated.
     * @return A list of IDs of the generated email communications.
     * @throws DomainEntityNotFoundException If the LatePaymentFine or its associated entities (Customer, Communication Data) are not found.
     * @throws ClientException               If any error occurs during the email or document generation process.
     */
    private List<Long> generateEmailAndDoc(Long latePaymentFineId) {
        try {
            List<Long> emailIds = new ArrayList<>();
            LatePaymentFine latePaymentFine = latePaymentFineRepository.findById(latePaymentFineId)
                    .orElseThrow(() -> new DomainEntityNotFoundException("Late Payment Fine not found;"));

            LatePaymentFineDocumentModel latePaymentFineDocumentModel = buildModelWithSharedInfo(new LatePaymentFineDocumentModel());
            setCustomerInfo(latePaymentFineDocumentModel, latePaymentFineId);
            setDocumentParams(latePaymentFineDocumentModel, latePaymentFine);
            setOverdueDocumentParams(latePaymentFineDocumentModel, latePaymentFineId);
            setInterestParams(latePaymentFineDocumentModel, latePaymentFineId);
            Customer customer = customerRepository.findById(latePaymentFine.getCustomerId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Late Payment Fine Customer not found;"));

            List<CommunicationDataResponse> appropriateCommunicationDataList = findAppropriateCommunicationData(latePaymentFine);
            for (CommunicationDataResponse appropriateCommunicationData : appropriateCommunicationDataList) {
                DocumentEmailCommunicationCreateRequest documentEmailCommunicationCreateRequest = new DocumentEmailCommunicationCreateRequest();
                documentEmailCommunicationCreateRequest.setCustomerCommunicationId(appropriateCommunicationData.getId());
                documentEmailCommunicationCreateRequest.setCustomerEmailAddress(appropriateCommunicationData.getContactValues());
                Long customerDetailId = customerDetailsRepository.findLastCustomerDetailIdByCustomerId(latePaymentFine.getCustomerId());
                documentEmailCommunicationCreateRequest.setCustomerDetailId(customerDetailId);

                Optional<ContractTemplate> emailTemplateOptional = contractTemplateRepository
                        .findByTemplatePurposeAndDefaultForLatePaymentFineEmail(ContractTemplatePurposes.LATE_PAYMENT_FINE, true);

                prepareEmailData(emailTemplateOptional, customer, documentEmailCommunicationCreateRequest, latePaymentFineDocumentModel, latePaymentFine, customerDetailId);
                prepareDocumentData(latePaymentFine, customer, latePaymentFineDocumentModel, customerDetailId, documentEmailCommunicationCreateRequest);

                findAndSetCommunicationTopicAndEmailBox(documentEmailCommunicationCreateRequest);
                Long emailFromDocument = emailCommunicationService.createEmailFromDocument(documentEmailCommunicationCreateRequest, false);
                emailIds.add(emailFromDocument);

                log.info("Created email from document with id %s".formatted(emailFromDocument));
            }

            saveLatePaymentFineCommunications(emailIds, latePaymentFineId);

            return emailIds;
        } catch (Exception e) {
            log.error("Exception while storing template file: {},{}", e.getMessage(), latePaymentFineId);
            throw new ClientException("Exception while storing template file: %s".formatted(e.getMessage()), APPLICATION_ERROR);
        }
    }

    /**
     * Prepares document data for a late payment fine by checking if the document already exists,
     * finding the appropriate template, generating the document, and adding it as an email attachment.
     *
     * @param latePaymentFine                         the late payment fine entity associated with the document data
     * @param customer                                the customer associated with the late payment fine
     * @param latePaymentFineDocumentModel            the model containing details for document generation
     * @param customerDetailId                        the ID of the customer's details
     * @param documentEmailCommunicationCreateRequest the request object for email communication containing attachments
     * @throws Exception if there is an error during document generation or processing
     */
    private void prepareDocumentData(LatePaymentFine latePaymentFine, Customer customer, LatePaymentFineDocumentModel latePaymentFineDocumentModel, Long customerDetailId, DocumentEmailCommunicationCreateRequest documentEmailCommunicationCreateRequest) throws Exception {
        if (!latePaymentFineDocumentFileRepository.existsLatePaymentFineDocumentFileByLatePaymentId(latePaymentFine.getId())) {
            Optional<ContractTemplate> documentTemplateOptional = contractTemplateRepository
                    .findByTemplatePurposeAndDefaultForLatePaymentFineDocument(ContractTemplatePurposes.LATE_PAYMENT_FINE, true);

            if (documentTemplateOptional.isPresent()) {
                ContractTemplate documentTemplate = documentTemplateOptional.get();
                Optional<ContractTemplateDetail> documentTemplateDetailOptional = contractTemplateDetailsRepository.findById(documentTemplate.getLastTemplateDetailId());
                if (documentTemplateDetailOptional.isPresent()) {
                    ContractTemplateDetail documentTemplateDetails = documentTemplateDetailOptional.get();
                    if (isCustomerTypeValid(customer, customerDetailId, documentTemplateDetails)) {
                        FileInfoShortResponse fileInfoShortResponse = generateAndSaveTemplate(
                                latePaymentFineDocumentModel,
                                latePaymentFine,
                                documentTemplateDetails,
                                FileFormat.PDF,
                                true,
                                customerDetailId
                        );
                        EmailCommunicationAttachment attachment = new EmailCommunicationAttachment();
                        attachment.setName(fileInfoShortResponse.fileName());
                        attachment.setFileUrl(fileInfoShortResponse.fileUrl());
                        attachment.setStatus(EntityStatus.ACTIVE);
                        documentEmailCommunicationCreateRequest.setAttachmentFileIds(Set.of(emailCommunicationAttachmentRepository.saveAndFlush(attachment).getId()));
                    }
                }
            }
        }
    }

    /**
     * Prepares the email data required for communication related to late payment fines.
     *
     * @param emailTemplateOptional                   Optional containing the email template if present.
     * @param customer                                The customer who is associated with the late payment fine.
     * @param documentEmailCommunicationCreateRequest Request object to store the email subject and body.
     * @param latePaymentFineDocumentModel            The document model for the late payment fine.
     * @param latePaymentFine                         The late payment fine object containing relevant payment information.
     * @param customerDetailId                        The ID of the customer details to be used in template generation.
     * @throws Exception If any error occurs during template generation, file download or docx parsing.
     */
    private void prepareEmailData(Optional<ContractTemplate> emailTemplateOptional, Customer customer, DocumentEmailCommunicationCreateRequest documentEmailCommunicationCreateRequest, LatePaymentFineDocumentModel latePaymentFineDocumentModel, LatePaymentFine latePaymentFine, Long customerDetailId) throws Exception {
        if (emailTemplateOptional.isPresent()) {
            ContractTemplate emailTemplate = emailTemplateOptional.get();
            Optional<ContractTemplateDetail> emailTemplateDetailsOptional = contractTemplateDetailsRepository.findById(emailTemplate.getLastTemplateDetailId());
            if (emailTemplateDetailsOptional.isPresent()) {
                ContractTemplateDetail emailTemplateDetails = emailTemplateDetailsOptional.get();
                if (isCustomerTypeValid(customer, customerDetailId, emailTemplateDetails)) {
                    documentEmailCommunicationCreateRequest.setEmailSubject(emailTemplateDetails.getSubject());
                    FileInfoShortResponse fileInfoShortResponse = generateAndSaveTemplate(
                            latePaymentFineDocumentModel,
                            latePaymentFine,
                            emailTemplateDetails,
                            FileFormat.DOCX,
                            false,
                            customerDetailId
                    );
                    ByteArrayResource byteArrayResource = fileService.downloadFile(fileInfoShortResponse.fileUrl());
                    String emailBody = parseDocxToHtml(byteArrayResource.getByteArray());
                    documentEmailCommunicationCreateRequest.setEmailBody(emailBody);
                }
            }
        }
    }

    /**
     * Validates whether the customer's type is valid based on the provided customer details and contract template details.
     *
     * @param customer               the customer object whose type needs to be validated
     * @param customerDetailId       the ID of the customer details to retrieve information from
     * @param contractTemplateDetail the contract template detail containing the allowed customer types
     * @return true if the customer's type is valid or matches the criteria in the contract template details, false otherwise
     */
    private Boolean isCustomerTypeValid(Customer customer, Long customerDetailId, ContractTemplateDetail contractTemplateDetail) {
        CustomerType customerType = customer.getCustomerType();
        Optional<CustomerDetails> customerDetails = customerDetailsRepository.findById(customerDetailId);
        if (customerDetails.isPresent()) {
            if (customerType == CustomerType.PRIVATE_CUSTOMER && Boolean.TRUE.equals(customerDetails.get().getBusinessActivity())) {
                customerType = CustomerType.PRIVATE_CUSTOMER_WITH_BUSINESS_ACTIVITY;
            }
        }
        return Objects.isNull(contractTemplateDetail.getCustomerType()) || contractTemplateDetail.getCustomerType().contains(customerType);
    }

    /**
     * Sets the customer information for the given late payment fine document model.
     *
     * @param latePaymentFineDocumentModel the model object where customer information will be stored
     * @param latePaymentFineId            the ID used to retrieve customer information from the repository
     */
    private void setCustomerInfo(LatePaymentFineDocumentModel latePaymentFineDocumentModel, Long latePaymentFineId) {
        LatePaymentFineCustomerInfoResponse customerInfoResponse = latePaymentFineRepository.getCustomerInfoResponse(latePaymentFineId);
        latePaymentFineDocumentModel.setCustomerAdditionalInfo(customerInfoResponse.getCustomerAdditionalInfo());
        latePaymentFineDocumentModel.setCustomerAddressComb(customerInfoResponse.getCustomerAddressComb());
        latePaymentFineDocumentModel.setCustomerApartment(customerInfoResponse.getCustomerApartment());
        latePaymentFineDocumentModel.setCustomerBlock(customerInfoResponse.getCustomerBlock());
        latePaymentFineDocumentModel.setCustomerDistrict(customerInfoResponse.getCustomerDistrict());
        latePaymentFineDocumentModel.setCustomerEntrance(customerInfoResponse.getCustomerEntrance());
        latePaymentFineDocumentModel.setCustomerFloor(customerInfoResponse.getCustomerFloor());
        latePaymentFineDocumentModel.setCustomerIdentifier(customerInfoResponse.getCustomerIdentifier());
        latePaymentFineDocumentModel.setCustomerNameComb(customerInfoResponse.getCustomerNameComb());
        latePaymentFineDocumentModel.setCustomerNameCombTrsl(customerInfoResponse.getCustomerNameCombTrsl());
        latePaymentFineDocumentModel.setCustomerNumber(customerInfoResponse.getCustomerNumber());
        latePaymentFineDocumentModel.setCustomerPopulatedPlace(customerInfoResponse.getCustomerPopulatedPlace());
        latePaymentFineDocumentModel.setCustomerQuarterRaName(customerInfoResponse.getCustomerQuarterRaName());
        latePaymentFineDocumentModel.setCustomerQuarterRaType(customerInfoResponse.getCustomerQuarterRaType());
        latePaymentFineDocumentModel.setCustomerSegments(customerInfoResponse.getCustomerSegments());
        latePaymentFineDocumentModel.setCustomerStrBlvdName(customerInfoResponse.getCustomerStrBlvdName());
        latePaymentFineDocumentModel.setCustomerStrBlvdNumber(customerInfoResponse.getCustomerStrBlvdNumber());
        latePaymentFineDocumentModel.setCustomerStrBlvdType(customerInfoResponse.getCustomerStrBlvdType());
        latePaymentFineDocumentModel.setCustomerVat(customerInfoResponse.getCustomerVat());
        latePaymentFineDocumentModel.setCustomerZip(customerInfoResponse.getCustomerZip());
    }

    /**
     * Sets the document parameters for the given LatePaymentFineDocumentModel based on the provided LatePaymentFine object.
     * This method updates the document model with details from the fine such as document number, date, type, due date, and amounts.
     * It also fetches and sets currency and billing group details if available.
     *
     * @param latePaymentFineDocumentModel The model object that represents the data structure to hold document parameters.
     * @param latePaymentFine              The LatePaymentFine object containing the data from which to extract document parameters.
     */
    private void setDocumentParams(LatePaymentFineDocumentModel latePaymentFineDocumentModel, LatePaymentFine latePaymentFine) {
        if (latePaymentFine.getType().equals(LatePaymentFineType.LATE_PAYMENT_FINE)) {
            latePaymentFineDocumentModel.setDocumentNumber(latePaymentFine.getLatePaymentNumber());
            latePaymentFineDocumentModel.setDocumentPrefix(latePaymentFine.getLatePaymentNumber().split("-")[0]);
            latePaymentFineDocumentModel.setDocumentDate(latePaymentFine.getCreateDate());
            latePaymentFineDocumentModel.setDocumentType(latePaymentFine.getType());
        } else {
            latePaymentFineDocumentModel.setReversedDocumentNumber(latePaymentFine.getLatePaymentNumber());
            latePaymentFineDocumentModel.setReversedDocumentPrefix(latePaymentFine.getLatePaymentNumber().split("-")[0]);
            latePaymentFineDocumentModel.setReversedDocumentDate(latePaymentFine.getCreateDate());
            latePaymentFineDocumentModel.setReversedDocumentType(latePaymentFine.getType());
        }

        latePaymentFineDocumentModel.setDueDate(latePaymentFine.getDueDate());
        latePaymentFineDocumentModel.setTotalAmount(latePaymentFine.getAmount());
        latePaymentFineDocumentModel.setTotalAmountOtherCurrency(latePaymentFine.getAmountInOtherCcy());

        Optional<Currency> currencyOptional = currencyRepository.findById(latePaymentFine.getCurrencyId());
        if (currencyOptional.isPresent()) {
            Currency currency = currencyOptional.get();
            latePaymentFineDocumentModel.setCurrencyPrintName(currency.getPrintName());
            latePaymentFineDocumentModel.setCurrencyAbr(currency.getAbbreviation());
            latePaymentFineDocumentModel.setCurrencyFullName(currency.getFullName());
            if (Objects.nonNull(currency.getAltCurrencyId())) {
                Optional<Currency> otherCurrencyOptional = currencyRepository.findById(currency.getAltCurrencyId());
                if (otherCurrencyOptional.isPresent()) {
                    Currency otherCurrency = otherCurrencyOptional.get();
                    latePaymentFineDocumentModel.setOtherCurrencyPrintName(otherCurrency.getPrintName());
                    latePaymentFineDocumentModel.setOtherCurrencyAbr(otherCurrency.getAbbreviation());
                    latePaymentFineDocumentModel.setOtherCurrencyFullName(otherCurrency.getFullName());
                }
            }

            if (Objects.nonNull(latePaymentFine.getContractBillingGroupId())) {
                Optional<ContractBillingGroup> billingGroupOptional = contractBillingGroupRepository.findById(latePaymentFine.getContractBillingGroupId());
                billingGroupOptional.ifPresent(contractBillingGroup -> latePaymentFineDocumentModel.setBillingGroup(contractBillingGroup.getGroupNumber()));
            }
        }

    }

    /**
     * Sets the parameters of an overdue document into the provided LatePaymentFineDocumentModel
     * using information retrieved from a LatePaymentFineOutDocInfoResponse object, based on the given late payment fine ID.
     *
     * @param latePaymentFineDocumentModel the model to be populated with overdue document parameters
     * @param latePaymentFineId            the identifier for retrieving the overdue document information
     */
    private void setOverdueDocumentParams(LatePaymentFineDocumentModel latePaymentFineDocumentModel, Long latePaymentFineId) {
        LatePaymentFineOutDocInfoResponse outDocInfo = latePaymentFineRepository.getOutDocInfo(latePaymentFineId);
        if (Objects.nonNull(outDocInfo)) {
            latePaymentFineDocumentModel.setFullPaymentDate(outDocInfo.getFullPaymentDate());
            latePaymentFineDocumentModel.setOverdueDocumentType(outDocInfo.getOverdueDocumentType());
            latePaymentFineDocumentModel.setOverdueDocumentNumber(outDocInfo.getOverdueDocumentNumber());
            latePaymentFineDocumentModel.setOverdueDocumentPrefix(outDocInfo.getOverdueDocumentPrefix());
            latePaymentFineDocumentModel.setOverdueDocumentDate(outDocInfo.getOverdueDocumentDate());
            latePaymentFineDocumentModel.setLiabilityInitialAmount(outDocInfo.getLiabilityInitialAmount());
        }

    }

    /**
     * Sets the interest parameters for a late payment fine document model based on the information
     * retrieved using the specified late payment fine ID. It populates the document model
     * with interest details such as interest amount, rate, and overdue information.
     *
     * @param latePaymentFineDocumentModel the document model that will be populated with interest data
     * @param latePaymentFineId            the ID of the late payment fine used to retrieve interest information
     */
    private void setInterestParams(LatePaymentFineDocumentModel latePaymentFineDocumentModel, Long latePaymentFineId) {
        List<LatePaymentFineInterestsMiddleResponse> interestsInfo = latePaymentFineRepository.getInterestsInfo(latePaymentFineId);
        List<LatePaymentFineInterestsResponse> latePaymentFineInterestsResponses = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(interestsInfo)) {
            for (LatePaymentFineInterestsMiddleResponse interest : interestsInfo) {
                LatePaymentFineInterestsResponse interestsResponse = new LatePaymentFineInterestsResponse();
                interestsResponse.setInterestAmount(interest.getInterestAmount());
                interestsResponse.setInterestRate(interest.getInterestRate());
                interestsResponse.setNumberDays(interest.getNumberDays());
                interestsResponse.setOverdueAmount(interest.getOverdueAmount());
                interestsResponse.setOverdueDocumentNumber(interest.getOverdueDocumentNumber());
                interestsResponse.setOverdueDocumentPrefix(interest.getOverdueDocumentPrefix());
                interestsResponse.setOverdueEndDate(interest.getOverdueEndDate());
                interestsResponse.setOverdueStartDate(interest.getOverdueStartDate());
                latePaymentFineInterestsResponses.add(interestsResponse);
            }
            latePaymentFineDocumentModel.setInterests(latePaymentFineInterestsResponses);
        }
    }

    /**
     * Generates a document based on the given template and late payment fine details, and optionally saves it.
     *
     * @param latePaymentFineDocumentModel the model containing data for the late payment fine document
     * @param latePaymentFine              the entity representing the late payment fine details
     * @param templateDetail               the template to be used for document generation
     * @param fileFormat                   the desired file format for the output document
     * @param saveFile                     a flag indicating whether the generated document should be saved
     * @param CustomerDetailId             the ID of the customer detail for which the document is generated
     * @return a FileInfoShortResponse object containing the file name and path of the generated document
     * @throws Exception if the document generation or saving process encounters an issue
     */
    private FileInfoShortResponse generateAndSaveTemplate(
            LatePaymentFineDocumentModel latePaymentFineDocumentModel,
            LatePaymentFine latePaymentFine,
            ContractTemplateDetail templateDetail,
            FileFormat fileFormat,
            Boolean saveFile,
            Long CustomerDetailId
    ) throws Exception {
        String fileName = formatDocumentFileName(latePaymentFine, templateDetail, CustomerDetailId);
        ByteArrayResource templateFileResource = createTemplateFileResource(templateDetail);
        DocumentPathPayloads documentPathPayloads = documentGenerationService.generateDocument(
                templateFileResource,
                buildFileDestinationPath(),
                fileName,
                latePaymentFineDocumentModel,
                Set.of(fileFormat),
                false
        );

        String fileUrl = fileFormat.equals(FileFormat.PDF) ? documentPathPayloads.pdfPath() : documentPathPayloads.docXPath();

        if (saveFile) {
            List<DocumentSigners> documentSigners = getDocumentSigners(templateDetail);
            Document document = saveDocument(
                    fileName,
                    fileUrl,
                    fileFormat,
                    documentSigners,
                    templateDetail.getTemplateId()
            );

            latePaymentFineDocumentFileRepository.save(
                    LatePaymentFineDocumentFile.builder()
                            .name(fileName)
                            .fileUrl(fileUrl)
                            .status(EntityStatus.ACTIVE)
                            .latePaymentId(latePaymentFine.getId())
                            .documentId(document.getId())
                            .build()
            );

            if (FileFormat.PDF.equals(fileFormat)) {
                signerChainManager.startSign(List.of(document));
            }
            fileUrl = document.getSignedFileUrl();
        }
        return new FileInfoShortResponse(fileName, fileUrl);
    }

    /**
     * Finds and sets the communication topic and email box for a document email communication request.
     * The method attempts to find a hardcoded "Late Payment Fine" topic with an active status
     * and sets it in the request if present. It also attempts to find an email box marked for
     * sending invoices, setting it if found. In the absence of such an email box, it attempts
     * to set a hardcoded email box by name.
     *
     * @param documentEmailCommunicationCreateRequest the request object for creating a document email communication
     */
    private void findAndSetCommunicationTopicAndEmailBox(DocumentEmailCommunicationCreateRequest documentEmailCommunicationCreateRequest) {
        Optional<TopicOfCommunication> topicOfCommunicationOptional = topicOfCommunicationRepository.findByNameAndStatusAndIsHardcodedTrue("Late Payment Fine", NomenclatureItemStatus.ACTIVE);
        topicOfCommunicationOptional.ifPresent(topicOfCommunication -> documentEmailCommunicationCreateRequest.setCommunicationTopicId(topicOfCommunication.getId()));

        Optional<EmailMailboxes> emailBoxOptional = emailMailboxesRepository.findByEmailForSendingInvoicesTrue();
        if (emailBoxOptional.isPresent()) {
            documentEmailCommunicationCreateRequest.setEmailBoxId(emailBoxOptional.get().getId());
        } else {
            Optional<EmailMailboxes> hardCodedEmail = emailMailboxesRepository.findByIsHardCodedTrue();
            hardCodedEmail.ifPresent(emailMailboxes -> documentEmailCommunicationCreateRequest.setEmailBoxId(emailMailboxes.getId()));
        }

    }

    /**
     * Finds and returns a list of appropriate communication data based on the provided late payment fine information.
     * It checks the contract billing group and customer details to determine the appropriate communication channels
     * and filters out duplicates if necessary.
     *
     * @param latePaymentFine the details of the late payment fine containing information such as
     *                        the customer ID and contract billing group ID.
     * @return a list of CommunicationDataResponse objects containing the necessary communication information
     * for email documents, ensuring unique contact values where applicable.
     */
    private List<CommunicationDataResponse> findAppropriateCommunicationData(LatePaymentFine latePaymentFine) {
        List<CommunicationDataResponse> communicationDataListForEmailDocument = new ArrayList<>();
        Long billingGroupCustomerId = null;
        if (Objects.nonNull(latePaymentFine.getContractBillingGroupId())) {
            Optional<ContractBillingGroup> billingGroup = contractBillingGroupRepository.findById(latePaymentFine.getContractBillingGroupId());
            if (billingGroup.isPresent() && Objects.nonNull(billingGroup.get().getAlternativeRecipientCustomerDetailId())) {
                communicationDataListForEmailDocument.add(new CommunicationDataResponse(customerCommunicationsRepository.findCustomerCommunicationAndEmail(billingGroup.get().getBillingCustomerCommunicationId())));
                Optional<CustomerDetails> customerDetails = customerDetailsRepository.findById(billingGroup.get().getAlternativeRecipientCustomerDetailId());
                if (customerDetails.isPresent()) {
                    billingGroupCustomerId = customerDetails.get().getCustomerId();
                }
            }
        }
        if (!latePaymentFine.getCustomerId().equals(billingGroupCustomerId)) {
            CommunicationDataMiddleResponse communicationDataId = customerRepository
                    .getCustomerCommunicationDataListForDocument(
                            latePaymentFine.getCustomerId(),
                            communicationContactPurposeProperties.getBillingCommunicationId(),
                            CustomerCommContactTypes.EMAIL.name()
                    );
            if (Objects.nonNull(communicationDataId)) {
                communicationDataListForEmailDocument.add(new CommunicationDataResponse(communicationDataId));
            }
        }

        if (communicationDataListForEmailDocument.size() == 2) {
            String firstContactValues = communicationDataListForEmailDocument.get(0).getContactValues();
            String secondContactValues = communicationDataListForEmailDocument.get(1).getContactValues();

            if (isValidContactValues(firstContactValues) && isValidContactValues(secondContactValues)) {
                String[] uniqueEmailsInSecond = getUniqueEmails(secondContactValues, firstContactValues);
                communicationDataListForEmailDocument.get(1).setContactValues(String.join(";", uniqueEmailsInSecond));
            }
        }

        return communicationDataListForEmailDocument;
    }

    /**
     * Validates whether the provided contact values string is non-null and not empty.
     *
     * @param contactValues the contact values string to be validated
     * @return true if the contact values string is non-null and not empty; otherwise, false
     */
    private boolean isValidContactValues(String contactValues) {
        return Objects.nonNull(contactValues) && !contactValues.isEmpty();
    }

    /**
     * Processes the provided contact and comparison email values and returns an array of email strings
     * that are unique to the contact values.
     *
     * @param contactValues    a semicolon-separated string of email addresses representing the primary list.
     * @param comparisonValues a semicolon-separated string of email addresses which will be used to filter out
     *                         any contacts found in this list from the contactValues.
     * @return an array of email strings that are present in the contactValues but not in the comparisonValues.
     */
    private String[] getUniqueEmails(String contactValues, String comparisonValues) {
        var comparisonEmails = comparisonValues.split(";");
        return Arrays
                .stream(contactValues.split(";"))
                .filter(email -> !Arrays.asList(comparisonEmails).contains(email))
                .toArray(String[]::new);
    }

    /**
     * Formats a document file name by combining various extracted components such as file prefix,
     * name, and suffix. It ensures the total length of the file name does not exceed a specified
     * maximum length and appends the appropriate file format suffix.
     *
     * @param latePaymentFine        an instance of LatePaymentFine, providing necessary details for file name extraction.
     * @param contractTemplateDetail an instance of ContractTemplateDetail, used to extract file prefix and other details.
     * @param customerDetailId       a Long representing the customer detail identifier used in file name extraction.
     * @return a formatted String representing the document file name including a file extension,
     * adjusted for length and invalid characters.
     */
    public String formatDocumentFileName(LatePaymentFine latePaymentFine, ContractTemplateDetail contractTemplateDetail, Long customerDetailId) {
        log.debug("Starting file name formatting");
        List<String> fileNameParts = new ArrayList<>();
        log.debug("Extracting file prefix");
        fileNameParts.add(documentGenerationUtil.extractFilePrefix(contractTemplateDetail));
        log.debug("Extracting file name");
        fileNameParts.add(extractFileName(latePaymentFine, contractTemplateDetail, customerDetailId));
        log.debug("Extracting file suffix");
        fileNameParts.add(extractFileSuffix(latePaymentFine.getCreateDate(), contractTemplateDetail));
        fileNameParts.removeIf(StringUtils::isBlank);

        String fileParts = String.join("_", fileNameParts);
        fileParts = StringUtils.isBlank(fileParts) ? "LatePaymentFine" : fileParts;
        return fileParts.substring(0, Math.min(fileParts.length(), 200)).concat(".").concat(FileFormat.PDF.getSuffix()).replaceAll("/", "_");
    }

    /**
     * Extracts a file name based on provided contract template details, customer details and a late payment fine information.
     * It sorts and evaluates the contract template file names, constructing a suitable name using various attributes
     * from customer details and late payment fine information.
     *
     * @param latePaymentFine        the late payment fine object containing information like late payment number and create date
     * @param contractTemplateDetail the contract template detail that includes a list of contract template file names
     * @param customerDetailId       the identifier for customer details used to fetch customer-specific information
     * @return a constructed file name string based on the provided details or the late payment number if an error occurs or no applicable file names exist
     */
    private String extractFileName(LatePaymentFine latePaymentFine, ContractTemplateDetail contractTemplateDetail, Long customerDetailId) {
        try {
            List<ContractTemplateFileName> fileName = contractTemplateDetail
                    .getFileName()
                    .stream()
                    .sorted(Comparator.comparing(ContractTemplateFileName::getPriority))
                    .toList();

            if (org.springframework.util.CollectionUtils.isEmpty(fileName)) {
                return latePaymentFine.getLatePaymentNumber();
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
                        case DOCUMENT_NUMBER -> nameParts.add(latePaymentFine.getLatePaymentNumber());
                        case FILE_ID ->
                                nameParts.add(String.valueOf(latePaymentFineDocumentFileRepository.getNextIdValue()));
                        case TIMESTAMP -> nameParts.add(latePaymentFine.getCreateDate().toString());
                    }
                }

                return String.join("_", nameParts);
            }
        } catch (Exception e) {
            log.error("Exception handled while trying to extract file name", e);
            return latePaymentFine.getLatePaymentNumber();
        }
    }

    private void saveLatePaymentFineCommunications(List<Long> emailIds, Long latePaymentFineId) {
        List<LatePaymentFineCommunications> latePaymentFineCommunicationsList = new ArrayList<>();
        for (Long emailId : emailIds) {
            LatePaymentFineCommunications latePaymentFineCommunications = new LatePaymentFineCommunications();
            latePaymentFineCommunications.setLatePaymentId(latePaymentFineId);
            latePaymentFineCommunications.setEmailCommunicationId(emailId);
            latePaymentFineCommunicationsList.add(latePaymentFineCommunications);
        }
        latePaymentFineCommunicationsRepository.saveAll(latePaymentFineCommunicationsList);
    }

    @Override
    protected String folderPath() {
        return FOLDER_PATH;
    }
}
