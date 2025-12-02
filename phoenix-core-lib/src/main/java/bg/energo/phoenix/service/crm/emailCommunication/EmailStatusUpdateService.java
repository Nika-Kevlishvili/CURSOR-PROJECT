package bg.energo.phoenix.service.crm.emailCommunication;

import bg.energo.mass_comm.models.TaskStatusResponse;
import bg.energo.mass_comm.models.TaskStatusResult;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.crm.emailCommunication.EmailCommunication;
import bg.energo.phoenix.model.entity.crm.emailCommunication.EmailCommunicationCustomer;
import bg.energo.phoenix.model.entity.crm.emailCommunication.EmailCommunicationCustomerContact;
import bg.energo.phoenix.model.enums.crm.emailCommunication.EmailCommunicationChannelType;
import bg.energo.phoenix.model.enums.crm.emailCommunication.EmailCommunicationContactStatus;
import bg.energo.phoenix.model.enums.crm.emailCommunication.EmailCommunicationCustomerStatus;
import bg.energo.phoenix.model.enums.crm.emailCommunication.EmailCommunicationStatus;
import bg.energo.phoenix.repository.crm.emailCommunication.EmailCommunicationCustomerContactRepository;
import bg.energo.phoenix.repository.crm.emailCommunication.EmailCommunicationCustomerRepository;
import bg.energo.phoenix.repository.crm.emailCommunication.EmailCommunicationRepository;
import bg.energo.phoenix.service.crm.emailClient.EmailSenderService;
import bg.energo.phoenix.util.epb.EPBBatchUtils;
import bg.energo.phoenix.util.epb.EPBListUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailStatusUpdateService {
    private static final int THREAD_COUNT = 10;
    private static final int BATCH_MAX_COUNT = 200;

    private final EmailCommunicationRepository emailCommunicationRepository;
    private final EmailCommunicationCustomerRepository emailCommunicationCustomerRepository;
    private final EmailCommunicationCustomerContactRepository emailCommunicationCustomerContactRepository;
    private final EmailSenderService emailSenderService;

    /**
     * Runs a job to process email communication customer contacts.
     * This job is executed asynchronously in a new thread.
     * It fetches contacts based on their status and processes them in batches.
     */
    //    @Scheduled(fixedDelay = 30000, initialDelay = 30000)
    @Transactional
    public void runJob() {
        // Create a new thread to run the job asynchronously
        new Thread(() -> {
            try {
                log.debug("Starting email status check job");
                // Fetch the list of email communication customer contacts with specific statuses and non-null task_id

                List<EmailCommunicationCustomerContact> emailCommunicationCustomerContacts =
                        emailCommunicationCustomerContactRepository
                                .findAllByStatusInAndTaskIdNotNull(
                                        EPBListUtils.convertEnumListIntoStringListIfNotNull(
                                                List.of(
                                                        EmailCommunicationContactStatus.NEW,
                                                        EmailCommunicationContactStatus.PENDING,
                                                        EmailCommunicationContactStatus.PARTIAL_SUCCESS,
                                                        EmailCommunicationContactStatus.IN_PROCESS
                                                )
                                        )
                                );

                // Check if any contacts were fetched
                if (CollectionUtils.isNotEmpty(emailCommunicationCustomerContacts)) {
                    log.debug("Fetched {} email communication customer contacts", emailCommunicationCustomerContacts.size());

                    // Process the contacts in batches
                    EPBBatchUtils.submitItemsInBatches(
                            emailCommunicationCustomerContacts,
                            THREAD_COUNT,
                            BATCH_MAX_COUNT,
                            this::processEmailCommunicationCustomerContactBatch
                    );
                } else {
                    log.warn("No email communication customer contacts found with the specified statuses and task_id.");
                }

                log.debug("Email status check job completed");

            } catch (Exception e) {
                // Log any exceptions that occur during the job execution
                log.error("An error occurred while running the email status check job: {}", e.getMessage(), e);
            }
        }).start();
    }

    /**
     * Processes a batch of email communication customer contacts asynchronously, fetching and updating their statuses.
     * This method runs the status-fetching process in a separate asynchronous task, allowing other operations to continue concurrently.
     * It logs the start, successful completion, and any unexpected errors during the batch processing.
     *
     * @param contacts A list of email communication customer contacts to process.
     */
    private void processEmailCommunicationCustomerContactBatch(List<EmailCommunicationCustomerContact> contacts) {
        // Start an asynchronous task to fetch and update contact statuses
        log.debug("Started fetching and updating statuses for {} email communication customer contacts", contacts.size());
        fetchEmailCommunicationContactStatuses(contacts);
        log.debug("Successfully updated statuses for {} email communication customer contacts", contacts.size());
    }


    /**
     * Fetches the statuses of email communication customer contacts and updates their statuses accordingly.
     * This method checks the statuses of the provided contacts and updates them in the system based on the task results.
     *
     * @param contactsForStatusCheck A list of email communication customer contacts whose statuses need to be checked.
     */
    public void fetchEmailCommunicationContactStatuses(List<EmailCommunicationCustomerContact> contactsForStatusCheck) {
        // If no contacts are provided, log and skip the status check
        if (CollectionUtils.isEmpty(contactsForStatusCheck)) {
            log.debug("No email communication customer contacts provided, skipping status check.");
            return;
        }

        try {
            // Transform the list of contacts into a map where the key is the task ID (UUID) and the value is the contact
            Map<UUID, EmailCommunicationCustomerContact> uuidRequestMap = EPBListUtils.transformToMap(
                    contactsForStatusCheck,
                    contact -> UUID.fromString(contact.getTaskId())
            );
            log.debug("Prepared map of task IDs for status check: {}", uuidRequestMap.keySet());

            // Fetch the task status response for the given task IDs
            Optional<TaskStatusResponse> contactStatusesResponseOptional = emailSenderService.fetchContactStatuses(new ArrayList<>(uuidRequestMap.keySet()));
            if (contactStatusesResponseOptional.isPresent()) {
                TaskStatusResponse contactStatusResponse = contactStatusesResponseOptional.get();
                log.debug("Received task status response for {} task IDs.", contactStatusResponse.getStatus().size());

                // Update the statuses of the email communication customer contacts based on the task results
                List<EmailCommunicationCustomerContact> statusUpdatedContacts = contactStatusResponse
                        .getStatus()
                        .stream()
                        .map(taskResult -> updateContactStatus(uuidRequestMap, taskResult))
                        .filter(Objects::nonNull)  // Ensure that only non-null contacts are returned
                        .toList();

                log.debug("Updated statuses for {} email communication customer contacts.", statusUpdatedContacts.size());

                // Group the updated contacts by their email communication customer ID for further processing
                Map<Long, List<EmailCommunicationCustomerContact>> groupStatusUpdatedContactsByCustomer = statusUpdatedContacts
                        .stream()
                        .collect(Collectors.groupingBy(EmailCommunicationCustomerContact::getEmailCommunicationCustomerId));

                // Update the statuses of the email communication customer entities based on the updated contacts
                updateEmailCommunicationInnerStatuses(groupStatusUpdatedContactsByCustomer);
            } else {
                log.warn("No task status response received for the provided task IDs.");
            }

        } catch (Exception e) {
            log.error("An error occurred while checking task statuses: {}", e.getMessage(), e);
        }
    }

    /**
     * Updates the statuses of email communication and contacts for each email communication customer.
     * This method processes the provided map of email communication customers and their associated contacts to update their communication status.
     *
     * @param groupStatusUpdatedContactsByCustomer A map of email communication customer IDs and their associated contacts
     *                                             whose statuses have been updated.
     */
    private void updateEmailCommunicationInnerStatuses(Map<Long, List<EmailCommunicationCustomerContact>> groupStatusUpdatedContactsByCustomer) {
        log.info("Updating email communication inner statuses...");

        // Iterate through the email communication customer groups, where each group contains a list of associated contacts
        groupStatusUpdatedContactsByCustomer.forEach((emailCommunicationCustomerId, emailCommunicationCustomerContacts) -> {
            log.debug("Processing email communication customer ID: {}", emailCommunicationCustomerId);

            // Fetch the email communication customer by ID from the repository
            Optional<EmailCommunicationCustomer> emailCommunicationCustomerOptional = emailCommunicationCustomerRepository.findById(emailCommunicationCustomerId);
            if (emailCommunicationCustomerOptional.isPresent()) {
                EmailCommunicationCustomer emailCommunicationCustomer = emailCommunicationCustomerOptional.get();
                log.debug("Found email communication customer: {}", emailCommunicationCustomer.getId());

                // Check if at least one contact has been successfully sent
                boolean isAnySend = emailCommunicationCustomerContacts
                        .stream()
                        .anyMatch(contact -> EmailCommunicationContactStatus.SUCCESS.equals(contact.getStatus()));

                // If any contact is successful, update the email communication customer's status to SENT_SUCCESSFULLY
                if (isAnySend) {
                    log.info("At least one contact sent successfully for email communication customer ID: {}", emailCommunicationCustomerId);
                    emailCommunicationCustomer.setStatus(EmailCommunicationCustomerStatus.SENT_SUCCESSFULLY);

                    // Find the active email communication for this email communication customer and update its status to SENT_SUCCESSFULLY
                    Optional<EmailCommunication> emailCommunicationOptional = emailCommunicationRepository.findByIdAndEntityStatusAndCommunicationChannel(
                            emailCommunicationCustomer.getEmailCommunicationId(),
                            EntityStatus.ACTIVE,
                            EmailCommunicationChannelType.EMAIL
                    );

                    if (emailCommunicationOptional.isPresent()) {
                        EmailCommunication emailCommunication = emailCommunicationOptional.get();
                        emailCommunication.setEmailCommunicationStatus(EmailCommunicationStatus.SENT_SUCCESSFULLY);
                        emailCommunicationRepository.saveAndFlush(emailCommunication);
                        log.debug("Updated email communication status to SENT_SUCCESSFULLY for email communication customer ID: {}", emailCommunicationCustomerId);
                    } else {
                        log.warn("No active email communication found for email communication customer ID: {}", emailCommunicationCustomerId);
                    }

                    // Update the status of all non-successful contacts to CANCELED
                    emailCommunicationCustomerContacts
                            .stream()
                            .filter(contact -> !EmailCommunicationContactStatus.SUCCESS.equals(contact.getStatus()))
                            .forEach(contact -> {
                                        contact.setStatus(EmailCommunicationContactStatus.CANCELED);
                                        log.debug("Updated contact status to CANCELED for contact ID: {}", contact.getId());
                                    }
                            );

                } else {
                    // If no contact is successful, mark the email communication customer status as IN_PROGRESS
                    log.info("No contacts successfully sent, setting status to IN_PROGRESS for email communication customer ID: {}", emailCommunicationCustomerId);
                    emailCommunicationCustomer.setStatus(EmailCommunicationCustomerStatus.IN_PROGRESS);
                }

                // Persist the updated statuses of the email communication customer and contacts
                emailCommunicationCustomerContactRepository.saveAllAndFlush(emailCommunicationCustomerContacts);
                emailCommunicationCustomerRepository.saveAndFlush(emailCommunicationCustomer);
                log.debug("Saved updated email communication customer and contact statuses for email communication customer ID: {}", emailCommunicationCustomerId);
            } else {
                // Log a warning if the email communication customer could not be found
                log.warn("Email communication customer not found for ID: {}", emailCommunicationCustomerId);
            }
        });

        log.info("Completed updating email communication inner statuses.");
    }

    /**
     * Updates the status of an email communication customer contact based on the provided task result.
     * This method looks up the contact using the task ID, updates its status according to the task result,
     * and logs the appropriate messages.
     *
     * @param contactMap A map of task IDs to email communication customer contacts.
     * @param taskResult The result of a task containing the status to be set.
     * @return The updated email communication customer contact, or null if no contact was found.
     */
    private EmailCommunicationCustomerContact updateContactStatus(Map<UUID, EmailCommunicationCustomerContact> contactMap, TaskStatusResult taskResult) {
        // Retrieve the task ID from the task result
        UUID taskId = taskResult.getTaskId();

        // Find the corresponding contact from the map using the task ID
        EmailCommunicationCustomerContact contact = contactMap.get(taskId);

        // Check if the contact exists for the given task ID
        if (contact != null) {
            // Update the contact's status based on the task result
            contact.setStatus(EmailCommunicationContactStatus.fromClientStatus(taskResult.getStatus()));
            log.debug("Updated contact with TaskId {} to status {}", taskId, taskResult.getStatus());
        } else {
            // Log a warning if no contact is found for the given task ID
            log.warn("No contact found for TaskId {}", taskId);
        }

        // Return the updated contact (or null if no contact was found)
        return contact;
    }

}
