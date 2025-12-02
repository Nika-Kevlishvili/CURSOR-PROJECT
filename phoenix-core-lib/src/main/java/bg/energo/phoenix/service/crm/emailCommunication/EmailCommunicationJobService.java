package bg.energo.phoenix.service.crm.emailCommunication;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.documentModels.EmailAndSmsDocumentModel;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.crm.emailCommunication.EmailCommunication;
import bg.energo.phoenix.model.entity.crm.emailCommunication.EmailCommunicationCustomer;
import bg.energo.phoenix.model.entity.crm.emailCommunication.EmailCommunicationTemplates;
import bg.energo.phoenix.model.entity.template.ContractTemplate;
import bg.energo.phoenix.model.entity.template.ContractTemplateDetail;
import bg.energo.phoenix.model.enums.contract.ContractType;
import bg.energo.phoenix.model.enums.crm.emailCommunication.EmailCommunicationDocGenerationStatus;
import bg.energo.phoenix.repository.crm.emailCommunication.EmailCommunicationCustomerRepository;
import bg.energo.phoenix.repository.crm.emailCommunication.EmailCommunicationRepository;
import bg.energo.phoenix.repository.crm.emailCommunication.EmailCommunicationTemplateRepository;
import bg.energo.phoenix.repository.template.ContractTemplateDetailsRepository;
import bg.energo.phoenix.repository.template.ContractTemplateRepository;
import bg.energo.phoenix.service.document.EmailAndSmsDocumentCreationService;
import bg.energo.phoenix.service.document.EmailAndSmsDocumentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailCommunicationJobService {

    private final EmailCommunicationCustomerRepository emailCommunicationCustomerRepository;
    private final EmailAndSmsDocumentCreationService emailAndSmsDocumentCreationService;
    private final EmailCommunicationTemplateRepository emailCommunicationTemplateRepository;
    private final EmailCommunicationRepository emailCommunicationRepository;
    private final ContractTemplateRepository contractTemplateRepository;
    private final ContractTemplateDetailsRepository contractTemplateDetailsRepository;

    /**
     * Generates the body content for mass emails by processing each email communication that requires body generation.
     * This method retrieves the list of email communications from the repository that need body generation and processes
     * each of them. If no email communications are found, it logs a warning and exits without processing. If any error
     * occurs during processing, an error is logged.
     * The method is transactional to ensure that all database operations are completed successfully, or none of them are
     * committed in case of an exception.
     *
     * <p>Transaction Management: This method is marked with the {@link Transactional} annotation, ensuring that the
     * processing is done within a transactional context. If any exception occurs, the transaction will be rolled back.
     *
     */
    @Transactional
    public void generateBody() {
        log.info("Starting the body generation process for mass emails.");
        List<EmailCommunication> emailCommunications = emailCommunicationRepository.findEmailsForBodyGeneration();
        if (CollectionUtils.isEmpty(emailCommunications)) {
            log.warn("No email communications found for body generation.");
            return;
        }
        try {
            for (EmailCommunication currentEmail : emailCommunications) {
                processCurrentMassEmail(currentEmail);
            }
        } catch (Exception e) {
            log.error("Error in processing mass emails", e);
        }
    }

    /**
     * Processes a single mass email communication and generates the necessary documents for all associated customers.
     *
     * This method performs the following tasks:
     * <ul>
     *     <li>Retrieves the appropriate templates for the email communication.</li>
     *     <li>Processes each customer associated with the email communication and generates their respective documents.</li>
     *     <li>Updates the email communication status to indicate that document generation has been completed.</li>
     * </ul>
     *
     * <p>The method is marked as {@link Async}, meaning it is executed asynchronously in a separate thread. It also uses
     * the {@link Transactional} annotation with propagation set to {@link Propagation#REQUIRES_NEW}, ensuring that this
     * method is executed within its own transaction, independent of the caller's transaction.</p>
     *
     * @param currentEmail The email communication object to be processed. Contains the email template and customer
     *                     associations that need to be processed.
     *
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processCurrentMassEmail(EmailCommunication currentEmail) {
        log.info("Processing mass email communication with ID: {}", currentEmail.getId());
        Pair<ContractTemplate, ContractTemplateDetail> templatePair = getTemplatesForDocuments(currentEmail.getEmailTemplateId());
        if (templatePair != null) {
            List<EmailCommunicationCustomer> customers = emailCommunicationCustomerRepository.findAllByEmailCommunicationId(currentEmail.getId());
            for (EmailCommunicationCustomer currentCustomer : customers) {
                try {
                    processCurrentMassEmailCustomer(currentEmail.getId(), currentCustomer, templatePair);
                } catch (Exception e) {
                    log.error("Error processing email customer: {}", currentCustomer.getId(), e);
                }
            }
        } else {
            log.error("No templates found for email communication with ID: {}", currentEmail.getId());
        }
        currentEmail.setDocGenerationStatus(EmailCommunicationDocGenerationStatus.GENERATED);
        emailCommunicationRepository.saveAndFlush(currentEmail);
        log.info("Email communication with ID: {} has been processed and doc generation status updated.", currentEmail.getId());
    }

    /**
     * Processes the email communication for a specific customer, generating the email body and handling any attachments.
     *
     * This method performs the following tasks:
     * <ul>
     *     <li>Generates an email and SMS document request based on the provided customer.</li>
     *     <li>Uses the provided contract templates to generate the email body and save it.</li>
     *     <li>Retrieves active notification templates and generates and saves the attachment for each template.</li>
     * </ul>
     *
     * The method is annotated with {@link Async}, meaning it runs asynchronously in a separate thread. It also uses the
     * {@link Transactional} annotation with propagation set to {@link Propagation#REQUIRES_NEW}, ensuring that this method
     * runs in its own transactional context, independent of the caller's transaction.
     *
     * @param massEmailId The ID of the mass email communication being processed.
     * @param currentCustomer The customer for whom the email body and attachments are being generated.
     * @param templatePair A pair of contract templates and details used to generate the email content.
     *
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processCurrentMassEmailCustomer(
            Long massEmailId,
            EmailCommunicationCustomer currentCustomer,
            Pair<ContractTemplate, ContractTemplateDetail> templatePair
    ) {
        log.info("Starting process for email customer body with ID: {}", currentCustomer.getId());
        try {
            EmailAndSmsDocumentRequest documentRequest = buildEmailAndSmsDocumentRequest(currentCustomer);
            EmailAndSmsDocumentModel emailAndSmsDocumentModel = emailAndSmsDocumentCreationService.generateDocumentJsonModel(documentRequest);

            if (Objects.nonNull(templatePair.getValue())) {
                generateAndSaveBody(currentCustomer, templatePair.getValue(), emailAndSmsDocumentModel);
            }

            List<EmailCommunicationTemplates> notificationTemplates = emailCommunicationTemplateRepository.findAllByEmailCommIdAndStatus(
                    massEmailId,
                    EntityStatus.ACTIVE
            );
            if (CollectionUtils.isNotEmpty(notificationTemplates)) {
                for (EmailCommunicationTemplates curr : notificationTemplates) {
                    log.info("Generating and saving attachment for email customer {} with template ID: {}", currentCustomer.getId(), curr.getTemplateId());
                    emailAndSmsDocumentCreationService.generateAndSaveEmailCustomerAttachment(
                            curr.getTemplateId(),
                            currentCustomer.getCustomerDetailId(),
                            currentCustomer.getId(),
                            emailAndSmsDocumentModel
                    );
                }
            }
        } catch (Exception e) {
            log.error("Error processing email customer body and attachment for customer: {}", currentCustomer.getId(), e);
        }
    }

    /**
     * Generates the email body for a specific customer using the provided contract template and document model, and saves it to the customer entity.
     *
     * This method performs the following tasks:
     * <ul>
     *     <li>Generates the email body by passing the contract template and document model to the document creation service.</li>
     *     <li>If the email body is successfully generated, it updates the customer's email body field and saves the updated customer entity to the repository.</li>
     *     <li>If the email body is null, it logs a warning.</li>
     * </ul>
     *
     * @param customer The customer for whom the email body is being generated.
     * @param templateDetail The contract template details used to generate the email body.
     * @param emailAndSmsDocumentModel The model containing the necessary data for generating the email content.
     */
    private void generateAndSaveBody(
            EmailCommunicationCustomer customer,
            ContractTemplateDetail templateDetail,
            EmailAndSmsDocumentModel emailAndSmsDocumentModel
    ) {
        log.debug("Generating email body for email customer with ID: {}", customer.getId());
        String body = emailAndSmsDocumentCreationService.generateEmailCustomerBody(
                templateDetail,
                emailAndSmsDocumentModel
        );
        if (Objects.nonNull(body)) {
            log.info("Generated email body for email customer with ID: {}", customer.getId());
            customer.setEmailBody(body);
            emailCommunicationCustomerRepository.saveAndFlush(customer);
            log.info("Saved generated email body for email customer with ID: {}", customer.getId());
        } else {
            log.warn("Generated email body is null for email customer with ID: {}", customer.getId());
        }
    }

    /**
     * Retrieves the contract template and its corresponding template details based on the provided template ID.
     *
     * This method performs the following tasks:
     * <ul>
     *     <li>Fetches the contract template from the repository using the provided template ID.</li>
     *     <li>Fetches the contract template details based on the `lastTemplateDetailId` from the retrieved template.</li>
     *     <li>Throws a {@link DomainEntityNotFoundException} if either the template or the template details are not found.</li>
     *     <li>Logs the appropriate debug information when the templates are successfully found or when an error occurs.</li>
     * </ul>
     *
     * @param templateId The ID of the template to fetch.
     * @return A {@link Pair} containing the contract template and its details if found; otherwise, returns {@code null}.
     * @throws DomainEntityNotFoundException if the template or template details cannot be found.
     */
    private Pair<ContractTemplate, ContractTemplateDetail> getTemplatesForDocuments(Long templateId) {
        log.debug("Fetching templates for template ID: {}", templateId);
        ContractTemplate template;
        ContractTemplateDetail templateDetail;

        if (templateId != null) {
            template = contractTemplateRepository
                    .findById(templateId)
                    .orElseThrow(
                            () -> new DomainEntityNotFoundException("Can't find template with id: %s".formatted(templateId))
                    );
            log.debug("Found contract template with ID: {}", templateId);
            templateDetail = contractTemplateDetailsRepository
                    .findById(template.getLastTemplateDetailId())
                    .orElseThrow(
                            () -> new DomainEntityNotFoundException("Can't find template details with id: %s".formatted(template.getLastTemplateDetailId()))
                    );
            log.debug("Found contract template detail with ID: {} for templateId: {}", templateDetail.getId(), templateId);
            return Pair.of(template, templateDetail);
        }

        log.error("Template ID is null. No templates found.");
        return null;
    }

    /**
     * Builds an {@link EmailAndSmsDocumentRequest} for a specific email communication customer.
     *
     * This method performs the following tasks:
     * <ul>
     *     <li>Determines the contract type for the provided customer.</li>
     *     <li>Creates a new {@link EmailAndSmsDocumentRequest} object based on the contract type and customer details.</li>
     *     <li>If the document request creation fails (i.e., returns {@code null}), a new, empty document request is created.</li>
     *     <li>Sets the customer ID and contract type in the document request.</li>
     * </ul>
     *
     * @param customer The email communication customer for whom the document request is being built.
     * @return A populated {@link EmailAndSmsDocumentRequest} object with the contract type, customer ID, and other relevant details.
     */
    private EmailAndSmsDocumentRequest buildEmailAndSmsDocumentRequest(EmailCommunicationCustomer customer) {
        ContractType contractType = determineContractType(customer);

        EmailAndSmsDocumentRequest documentRequest = createWithContractDetails(contractType, customer);
        if (Objects.isNull(documentRequest)) {
            documentRequest = new EmailAndSmsDocumentRequest();
        }

        documentRequest.setEmailCommunicationCustomerId(customer.getId());
        documentRequest.setContractType(contractType);
        return documentRequest;
    }

    /**
     * Creates an {@link EmailAndSmsDocumentRequest} based on the contract type and customer details.
     *
     * This method generates an email and SMS document request by fetching the appropriate contract details based on the
     * provided contract type (either SERVICE_CONTRACT or PRODUCT_CONTRACT). The contract ID, number, and detail ID are
     * populated in the document request for the corresponding contract type.
     *
     * If the contract type is unsupported, it logs a warning and returns {@code null}.
     *
     * @param contractType The type of contract (either {@link ContractType#SERVICE_CONTRACT} or {@link ContractType#PRODUCT_CONTRACT}).
     * @param customer The email communication customer whose contract details are to be fetched.
     * @return A populated {@link EmailAndSmsDocumentRequest} object with contract details, or {@code null} if the contract type is unsupported.
     */
    private EmailAndSmsDocumentRequest createWithContractDetails(ContractType contractType, EmailCommunicationCustomer customer) {
        EmailAndSmsDocumentRequest documentRequest = new EmailAndSmsDocumentRequest();
        if (ContractType.SERVICE_CONTRACT.equals(contractType)) {
            List<Object[]> idAndNumber = emailCommunicationCustomerRepository.fetchServiceContractNumberByDetailId(customer.getServiceContractDetailId());
            Long contractId = (Long) idAndNumber.get(0)[0];
            String contractNumber = (String) idAndNumber.get(0)[1];
            documentRequest.setContractDetailId(customer.getServiceContractDetailId());
            documentRequest.setContractNumber(contractNumber);
            documentRequest.setContractId(contractId);
            return documentRequest;
        } else if (ContractType.PRODUCT_CONTRACT.equals(contractType)) {
            List<Object[]> idAndNumber = emailCommunicationCustomerRepository.fetchProductContractNumberByDetailId(customer.getProductContractDetailId());
            Long contractId = (Long) idAndNumber.get(0)[0];
            String contractNumber = (String) idAndNumber.get(0)[1];
            documentRequest.setContractDetailId(customer.getProductContractDetailId());
            documentRequest.setContractNumber(contractNumber);
            documentRequest.setContractId(contractId);
            return documentRequest;
        } else {
            log.warn("Unsupported contract type: {}", contractType);
            return null;
        }
    }

    /**
     * Determines the contract type for a given email communication customer based on their contract details.
     *
     * This method inspects the provided customer's contract details to determine the contract type:
     * <ul>
     *     <li>If the customer has a non-null product contract detail ID, it returns {@link ContractType#PRODUCT_CONTRACT}.</li>
     *     <li>If the customer has a non-null service contract detail ID, it returns {@link ContractType#SERVICE_CONTRACT}.</li>
     *     <li>If neither contract detail ID is present, it logs a warning and returns {@code null}.</li>
     * </ul>
     *
     * @param customerInfo The email communication customer whose contract type is to be determined.
     * @return The determined {@link ContractType}, or {@code null} if no valid contract type is found.
     */
    private ContractType determineContractType(EmailCommunicationCustomer customerInfo) {
        if (Objects.nonNull(customerInfo.getProductContractDetailId())) {
            return ContractType.PRODUCT_CONTRACT;
        } else if (Objects.nonNull(customerInfo.getServiceContractDetailId())) {
            return ContractType.SERVICE_CONTRACT;
        } else {
            log.warn("Email customer with ID: {} does not have a valid contract type.", customerInfo.getId());
            return null;
        }
    }
}
