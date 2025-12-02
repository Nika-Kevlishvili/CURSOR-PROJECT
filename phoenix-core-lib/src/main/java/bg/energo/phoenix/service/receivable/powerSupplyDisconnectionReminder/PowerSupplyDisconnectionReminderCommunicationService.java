package bg.energo.phoenix.service.receivable.powerSupplyDisconnectionReminder;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.documentModels.remiderForDcn.ReminderForDcnDocumentModel;
import bg.energo.phoenix.model.entity.contract.billing.ContractBillingGroup;
import bg.energo.phoenix.model.entity.crm.emailCommunication.EmailCommunication;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.customer.communication.CustomerCommunicationContacts;
import bg.energo.phoenix.model.entity.customer.communication.CustomerCommunications;
import bg.energo.phoenix.model.entity.nomenclature.crm.EmailMailboxes;
import bg.energo.phoenix.model.entity.nomenclature.crm.SmsSendingNumber;
import bg.energo.phoenix.model.entity.nomenclature.crm.TopicOfCommunication;
import bg.energo.phoenix.model.entity.receivable.customerLiability.CustomerLiability;
import bg.energo.phoenix.model.entity.receivable.powerSupplyDisconnectionReminder.PowerSupplyDcnReminderDocFiles;
import bg.energo.phoenix.model.entity.receivable.powerSupplyDisconnectionReminder.PowerSupplyDisconnectionReminder;
import bg.energo.phoenix.model.entity.receivable.powerSupplyDisconnectionReminder.PowerSupplyDisconnectionReminderCustomers;
import bg.energo.phoenix.model.entity.template.ContractTemplate;
import bg.energo.phoenix.model.entity.template.ContractTemplateDetail;
import bg.energo.phoenix.model.enums.customer.CustomerCommContactTypes;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.receivable.reminder.CommunicationChannel;
import bg.energo.phoenix.model.request.crm.emailCommunication.DocumentEmailCommunicationCreateRequest;
import bg.energo.phoenix.model.request.crm.smsCommunication.DocumentSmsCommunicationCreateRequest;
import bg.energo.phoenix.model.response.receivable.powerSupplyDisconnectionReminder.BillingGroupDataForPSDR;
import bg.energo.phoenix.repository.contract.billing.ContractBillingGroupRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractDetailsRepository;
import bg.energo.phoenix.repository.crm.emailCommunication.EmailCommunicationRepository;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommunicationContactsRepository;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommunicationsRepository;
import bg.energo.phoenix.repository.nomenclature.crm.EmailMailboxesRepository;
import bg.energo.phoenix.repository.nomenclature.crm.SmsSendingNumberRepository;
import bg.energo.phoenix.repository.nomenclature.crm.TopicOfCommunicationRepository;
import bg.energo.phoenix.repository.receivable.customerLiability.CustomerLiabilityRepository;
import bg.energo.phoenix.repository.receivable.powerSupplyDisconnectionReminder.PowerSupplyDcnReminderDocFileRepository;
import bg.energo.phoenix.repository.template.ContractTemplateDetailsRepository;
import bg.energo.phoenix.repository.template.ContractTemplateRepository;
import bg.energo.phoenix.service.crm.emailCommunication.EmailCommunicationService;
import bg.energo.phoenix.service.crm.smsCommunication.SmsCommunicationService;
import bg.energo.phoenix.service.document.ReminderForDcnDocumentCreationService;
import bg.energo.phoenix.service.document.enums.FileFormat;
import bg.energo.phoenix.service.document.ftpService.FileService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static bg.energo.phoenix.exception.ErrorCode.APPLICATION_ERROR;
import static bg.energo.phoenix.service.document.DocumentParserService.parseDocx;
import static bg.energo.phoenix.service.document.DocumentParserService.parseDocxToHtml;

@Service
@Slf4j
@RequiredArgsConstructor
public class PowerSupplyDisconnectionReminderCommunicationService {

    private static final String REMINDER_TOPIC_NAME = "Reminder for disconnection";

    private final CustomerDetailsRepository customerDetailsRepository;
    private final CustomerCommunicationContactsRepository customerCommunicationContactsRepository;
    private final TopicOfCommunicationRepository topicOfCommunicationRepository;
    private final EmailMailboxesRepository emailMailboxesRepository;

    private final EmailCommunicationService emailCommunicationService;
    private final SmsCommunicationService smsCommunicationService;
    private final ReminderForDcnDocumentCreationService reminderForDcnDocumentCreationService;
    private final FileService fileService;
    private final CustomerLiabilityRepository customerLiabilityRepository;
    private final ContractBillingGroupRepository contractBillingGroupRepository;
    private final ProductContractDetailsRepository productContractDetailsRepository;
    private final ContractTemplateRepository contractTemplateRepository;
    private final ContractTemplateDetailsRepository contractTemplateDetailsRepository;
    private final SmsSendingNumberRepository smsSendingNumberRepository;
    private final CustomerCommunicationsRepository customerCommunicationsRepository;
    private final PowerSupplyDcnReminderDocFileRepository powerSupplyDcnReminderDocFileRepository;
    private final EmailCommunicationRepository emailCommunicationRepository;

    /**
     * Generates email and SMS communications for a reminder
     */
    public void generateCommunications(
            PowerSupplyDisconnectionReminder reminder,
            List<PowerSupplyDisconnectionReminderCustomers> customers) {

        log.debug("Generating communications for reminder ID: {}", reminder.getId());

        if (reminder.getCommunicationChannels() == null || reminder.getCommunicationChannels().isEmpty()) {
            log.debug("No communication channels configured for reminder ID: {}", reminder.getId());
            return;
        }

        Map<Long, BillingGroupDataForPSDR> groupedData = groupCustomersWithCommunicationData(customers);

        for (Map.Entry<Long, BillingGroupDataForPSDR> entry : groupedData.entrySet()) {
            Long billingGroup = entry.getKey();
            BillingGroupDataForPSDR groupData = entry.getValue();

            try {
                processCustomerGroupCommunications(reminder, billingGroup, groupData);
            } catch (Exception e) {
                log.error("Error processing communications for billing group: {}", billingGroup, e);
            }
        }

        log.debug("Completed generating communications for reminder ID: {}", reminder.getId());
    }

    /**
     * Process communications for a group of customers with the same billing group
     */
    private void processCustomerGroupCommunications(
            PowerSupplyDisconnectionReminder reminder,
            Long billingGroupId,
            BillingGroupDataForPSDR groupData) {

        try {
            List<PowerSupplyDisconnectionReminderCustomers> customers = groupData.getCustomers();
            Long customerCommunicationsId = groupData.getCustomerCommunicationsId();

            log.debug("Processing communications for billing group: {} with {} customers",
                    billingGroupId, customers.size());

            Map<Long, List<PowerSupplyDisconnectionReminderCustomers>> customerGroups = customers.stream()
                    .collect(Collectors.groupingBy(PowerSupplyDisconnectionReminderCustomers::getCustomerId));

            ContractBillingGroup billingGroup = contractBillingGroupRepository.findById(billingGroupId)
                    .orElseThrow(() -> new DomainEntityNotFoundException("Billing group not found with ID: " + billingGroupId));

            if (billingGroup.getAlternativeRecipientCustomerDetailId() != null) {
                CustomerDetails altCustomerDetails = customerDetailsRepository.findById(billingGroup.getAlternativeRecipientCustomerDetailId())
                        .orElseThrow(() -> new DomainEntityNotFoundException("Alternative customer details not found with ID: " + billingGroup.getAlternativeRecipientCustomerDetailId()));

                Long altCustomerId = altCustomerDetails.getCustomerId();

                if (!customerGroups.containsKey(altCustomerId)) {
                    log.debug("Adding alternative customer with ID: {} to billing group: {}", altCustomerId, billingGroupId);

                    customerGroups.put(altCustomerId, new ArrayList<>());
                }
            }

            for (Map.Entry<Long, List<PowerSupplyDisconnectionReminderCustomers>> customerEntry : customerGroups.entrySet()) {
                Long customerId = customerEntry.getKey();

                CustomerCommunications customerCommunication = customerCommunicationsRepository.findById(customerCommunicationsId)
                        .orElseThrow(() -> new DomainEntityNotFoundException("Customer communication not found with ID: " + customerCommunicationsId));

                CustomerDetails customerDetails = customerDetailsRepository.findById(customerCommunication.getCustomerDetailsId())
                        .orElseThrow(() -> new DomainEntityNotFoundException("Customer details not found with ID: " + customerCommunication.getCustomerDetailsId()));

                List<CustomerCommunicationContacts> customerContacts = customerCommunicationContactsRepository
                        .findByCustomerCommIdAndStatuses(customerCommunicationsId, List.of(Status.ACTIVE));

                if (reminder.getCommunicationChannels().contains(CommunicationChannel.EMAIL) && reminder.getEmailTemplateId() != null) {
                    generateEmailNotification(reminder, billingGroupId, customerDetails, customerContacts, customerCommunicationsId);
                }

                if (reminder.getCommunicationChannels().contains(CommunicationChannel.SMS) && reminder.getSmsTemplateId() != null) {
                    generateSmsNotification(reminder, billingGroupId, customerDetails, customerContacts, customerCommunicationsId);
                }

                log.debug("Successfully processed communications for customer ID: {} in billing group: {}", customerId, billingGroupId);
            }

        } catch (Exception e) {
            log.error("Error processing communications for billing group: {}", billingGroupId, e);
        }
    }

    /**
     * Groups customers by their billing group and retrieves the communication data in one pass
     */
    private Map<Long, BillingGroupDataForPSDR> groupCustomersWithCommunicationData(
            List<PowerSupplyDisconnectionReminderCustomers> customers) {
        Map<Long, BillingGroupDataForPSDR> groupedData = new HashMap<>();

        for (PowerSupplyDisconnectionReminderCustomers customer : customers) {
            try {
                CustomerLiability liability = customerLiabilityRepository.findById(customer.getCustomerLiabilityId())
                        .orElseThrow(() -> new DomainEntityNotFoundException("Customer Liability not found with ID: " + customer.getCustomerLiabilityId()));

                Long billingGroupId = liability.getContractBillingGroupId();

                if (billingGroupId == null) {
                    log.debug("No billing group found for liability ID: {}", customer.getCustomerLiabilityId());
                    continue;
                }

                Long customerCommunicationsId = productContractDetailsRepository
                        .findCustomerCommunicationIdForBillingByLiabilityId(customer.getCustomerLiabilityId());

                if (customerCommunicationsId == null) {
                    log.debug("Could not determine communication ID for liability ID: {}", customer.getCustomerLiabilityId());
                    continue;
                }

                List<CustomerCommunicationContacts> contacts = customerCommunicationContactsRepository
                        .findByCustomerCommIdAndStatuses(customerCommunicationsId, List.of(Status.ACTIVE));

                if (contacts.isEmpty()) {
                    log.debug("No communication contacts found for communication ID: {}", customerCommunicationsId);
                    continue;
                }

                final Long finalCustomerCommunicationsId = customerCommunicationsId;
                BillingGroupDataForPSDR groupData = groupedData.computeIfAbsent(billingGroupId,
                        k -> new BillingGroupDataForPSDR(finalCustomerCommunicationsId));
                groupData.addCustomer(customer);
                groupData.setContacts(contacts);

            } catch (Exception e) {
                log.error("Error processing customer for billing group determination, ID: {}", customer.getId(), e);
            }
        }

        return groupedData;
    }

    /**
     * Generates email notification for a reminder billing group
     */
    @SneakyThrows
    private void generateEmailNotification(
            PowerSupplyDisconnectionReminder reminder,
            Long billingGroup,
            CustomerDetails customerDetails,
            List<CustomerCommunicationContacts> contacts,
            Long customerCommunicationsId) {

        log.debug("Generating email notification for reminder ID: {} and billing group: {}",
                reminder.getId(), billingGroup);

        List<String> emailAddresses = contacts.stream()
                .filter(contact -> contact.getContactType().equals(CustomerCommContactTypes.EMAIL))
                .filter(contact -> contact.getStatus().equals(Status.ACTIVE))
                .map(CustomerCommunicationContacts::getContactValue)
                .collect(Collectors.toList());

        if (emailAddresses.isEmpty()) {
            log.debug("No email addresses found for billing group: {}", billingGroup);
            return;
        }

        ContractTemplateDetail contractTemplateDetail = getContractTemplateLastDetails(reminder.getEmailTemplateId());

        Pair<ReminderForDcnDocumentModel, PowerSupplyDcnReminderDocFiles> pair =
                reminderForDcnDocumentCreationService.generateDocument(
                        reminder.getId(),
                        reminder.getEmailTemplateId(),
                        customerDetails.getCustomerId(),
                        FileFormat.DOCX,
                        true);

        String docxUrl = pair.getRight().getFileUrl();
        ByteArrayResource emailDocument = fileService.downloadFile(docxUrl);
        PowerSupplyDcnReminderDocFiles file = pair.getRight();
        powerSupplyDcnReminderDocFileRepository.delete(file);

        String emailBody = parseDocxToHtml(emailDocument.getByteArray());

        DocumentEmailCommunicationCreateRequest request = DocumentEmailCommunicationCreateRequest.builder()
                .emailSubject(contractTemplateDetail.getSubject())
                .emailBody(emailBody)
                .customerDetailId(customerDetails.getId())
                .customerCommunicationId(customerCommunicationsId)
                .customerEmailAddress(String.join(";", emailAddresses))
                .emailTemplateId(reminder.getEmailTemplateId())
                .build();

        TopicOfCommunication topicOfCommunication = topicOfCommunicationRepository
                .findByNameAndStatusAndIsHardcodedTrue(REMINDER_TOPIC_NAME, NomenclatureItemStatus.ACTIVE)
                .orElseThrow(() -> new DomainEntityNotFoundException("Topic of communication not found with name: " + REMINDER_TOPIC_NAME));

        request.setCommunicationTopicId(topicOfCommunication.getId());

        Optional<EmailMailboxes> emailBoxOptional = emailMailboxesRepository.findByEmailForSendingInvoicesTrue();
        if (emailBoxOptional.isPresent()) {
            request.setEmailBoxId(emailBoxOptional.get().getId());
        } else {
            Optional<EmailMailboxes> defaultEmail = emailMailboxesRepository.findByIsHardCodedTrue();
            defaultEmail.ifPresent(emailMailboxes -> request.setEmailBoxId(emailMailboxes.getId()));
        }

        try {
            Long emailId = emailCommunicationService.createEmailFromDocument(request, false);
            EmailCommunication emailCommunication = emailCommunicationRepository.findById(emailId)
                    .orElseThrow(() -> new DomainEntityNotFoundException("Email not found with id: " + emailId));
            emailCommunication.setSystemUserId("system");
            emailCommunicationRepository.saveAndFlush(emailCommunication);
            log.debug("Email created successfully with ID: {} for reminder ID: {}, billing group: {}",
                    emailId, reminder.getId(), billingGroup);
        } catch (Exception e) {
            log.error("Error creating email for reminder ID: {}, billing group: {}", reminder.getId(), billingGroup, e);
            throw new ClientException("Error creating email: " + e.getMessage(), APPLICATION_ERROR);
        }
    }

    /**
     * Generates SMS notification for a reminder billing group
     */
    @SneakyThrows
    private void generateSmsNotification(
            PowerSupplyDisconnectionReminder reminder,
            Long billingGroup,
            CustomerDetails customerDetails,
            List<CustomerCommunicationContacts> contacts,
            Long customerCommunicationsId) {

        log.debug("Generating SMS notification for reminder ID: {} and billing group: {}",
                reminder.getId(), billingGroup);

        List<String> mobileNumbers = contacts.stream()
                .filter(contact -> contact.getContactType().equals(CustomerCommContactTypes.MOBILE_NUMBER))
                .filter(contact -> contact.getStatus().equals(Status.ACTIVE))
                .filter(CustomerCommunicationContacts::isSendSms)
                .map(CustomerCommunicationContacts::getContactValue)
                .toList();

        if (mobileNumbers.isEmpty()) {
            log.debug("No SMS-enabled mobile numbers found for billing group: {}", billingGroup);
            return;
        }

        Pair<ReminderForDcnDocumentModel, PowerSupplyDcnReminderDocFiles> pair =
                reminderForDcnDocumentCreationService.generateDocument(
                        reminder.getId(),
                        reminder.getSmsTemplateId(),
                        customerDetails.getCustomerId(),
                        FileFormat.DOCX,
                        true);

        String docxUrl = pair.getRight().getFileUrl();
        ByteArrayResource smsDocument = fileService.downloadFile(docxUrl);
        PowerSupplyDcnReminderDocFiles file = pair.getRight();
        powerSupplyDcnReminderDocFileRepository.delete(file);

        String smsBody = parseDocx(smsDocument.getByteArray());

        for (String mobileNumber : mobileNumbers) {
            try {
                DocumentSmsCommunicationCreateRequest request = DocumentSmsCommunicationCreateRequest.builder()
                        .smsBody(smsBody)
                        .customerDetailId(customerDetails.getId())
                        .customerCommunicationId(customerCommunicationsId)
                        .customerPhoneNumber(mobileNumber)
                        .smsTemplateId(reminder.getSmsTemplateId())
                        .build();

                TopicOfCommunication topicOfCommunication = topicOfCommunicationRepository
                        .findByNameAndStatusAndIsHardcodedTrue(REMINDER_TOPIC_NAME, NomenclatureItemStatus.ACTIVE)
                        .orElseThrow(() -> new DomainEntityNotFoundException("Topic of communication not found with name: " + REMINDER_TOPIC_NAME));

                request.setCommunicationTopicId(topicOfCommunication.getId());

                Optional<SmsSendingNumber> smsNumberOptional = smsSendingNumberRepository.findByDefaultSelectionTrue();
                if (smsNumberOptional.isPresent()) {
                    request.setSmsNumberId(smsNumberOptional.get().getId());
                } else {
                    Optional<SmsSendingNumber> isHardCodedTrue = smsSendingNumberRepository.findByIsHardCodedTrue();
                    isHardCodedTrue.ifPresent(smsSendingNumber -> request.setSmsNumberId(smsSendingNumber.getId()));
                }

                Long smsId = smsCommunicationService.createSmsFromDocument(request, true);
                log.debug("SMS created successfully with ID: {} for reminder ID: {}, billing group: {}, mobile: {}",
                        smsId, reminder.getId(), billingGroup, mobileNumber);
            } catch (Exception e) {
                log.error("Error creating SMS for reminder ID: {}, billing group: {}, mobile: {}",
                        reminder.getId(), billingGroup, mobileNumber, e);
            }
        }
    }

    private ContractTemplateDetail getContractTemplateLastDetails(Long contractTemplateId) {
        return Optional
                .ofNullable(getContractTemplate(contractTemplateId))
                .map(ContractTemplate::getLastTemplateDetailId)
                .flatMap(lastTemplateDetailId -> Optional
                        .of(contractTemplateDetailsRepository.findById(lastTemplateDetailId))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                )
                .orElseThrow(
                        () -> new DomainEntityNotFoundException("Can't find template details for template id: %s".formatted(contractTemplateId))
                );
    }

    private ContractTemplate getContractTemplate(Long templateId) {
        return contractTemplateRepository
                .findById(templateId)
                .orElseThrow(
                        () -> new DomainEntityNotFoundException("Can't find template with id: %s".formatted(templateId))
                );
    }
}
