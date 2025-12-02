package bg.energo.phoenix.service.crm.emailCommunication;

import bg.energo.mass_comm.models.Attachment;
import bg.energo.mass_comm.models.SendEmailResponse;
import bg.energo.mass_comm.models.TaskStatus;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.crm.emailCommunication.EmailCommunication;
import bg.energo.phoenix.model.entity.crm.emailCommunication.EmailCommunicationCustomer;
import bg.energo.phoenix.model.entity.crm.emailCommunication.EmailCommunicationCustomerContact;
import bg.energo.phoenix.model.enums.crm.emailCommunication.EmailCommunicationContactStatus;
import bg.energo.phoenix.model.enums.crm.emailCommunication.EmailCommunicationCustomerStatus;
import bg.energo.phoenix.model.request.crm.emailCommunication.EmailSendContactModel;
import bg.energo.phoenix.model.request.crm.emailCommunication.EmailSendModel;
import bg.energo.phoenix.repository.crm.emailCommunication.EmailCommunicationAttachmentRepository;
import bg.energo.phoenix.repository.crm.emailCommunication.EmailCommunicationCustomerContactRepository;
import bg.energo.phoenix.repository.crm.emailCommunication.EmailCommunicationCustomerRepository;
import bg.energo.phoenix.repository.crm.emailCommunication.EmailCommunicationRepository;
import bg.energo.phoenix.service.crm.emailClient.EmailSenderService;
import bg.energo.phoenix.service.document.ftpService.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URLConnection;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailCommunicationSenderService {

    private final EmailCommunicationCustomerContactRepository emailCommunicationCustomerContactRepository;
    private final EmailCommunicationAttachmentRepository emailCommunicationAttachmentRepository;
    private final EmailCommunicationCustomerRepository emailCommunicationCustomerRepository;
    private final EmailCommunicationRepository emailCommunicationRepository;
    private final EmailSenderService emailSenderService;
    private final FileService fileService;

    /**
     * Sends a batch of emails asynchronously based on the provided list of {@link EmailSendModel} objects.
     * This method processes each {@link EmailSendModel} in the provided list, sending emails to the contacts specified in each model.
     * For each contact, it attempts to send an email using the {@link EmailSenderService}. The method logs the status of each email
     * and updates the contact status accordingly. It also updates the status of the associated {@link EmailCommunicationCustomer}
     * based on whether any of the emails were successfully sent.
     *
     * @param emailSendModel a list of {@link EmailSendContactModel} objects, where each object contains information about the email to be sent,
     *                       including recipient contacts, subject, body, and attachments.
     * @see EmailSendModel
     * @see EmailSenderService
     * @see EmailCommunicationCustomer
     * @see EmailCommunicationCustomerContact
     * @see Attachment
     * @see EmailCommunicationCustomerStatus
     * @see EmailCommunicationContactStatus
     */
    @Async
    public void sendBatch(EmailSendModel emailSendModel) {
        List<EmailSendContactModel> emailSendContactModels = emailSendModel.getEmailSendContactModels();
        List<Attachment> attachments = emailSendModel.getAttachments();
        String subject = emailSendModel.getSubject();
        String body = emailSendModel.getBody();

        for (EmailSendContactModel currentModel : emailSendContactModels) {
            EmailCommunicationCustomer emailCommunicationCustomer = currentModel.getEmailCommunicationCustomer();
            List<EmailCommunicationCustomerContact> emailCommunicationCustomerContacts = currentModel.getEmailCommunicationCustomerContacts();

            for (EmailCommunicationCustomerContact currentContact : emailCommunicationCustomerContacts) {
                Optional<SendEmailResponse> sendEmailResponseOptional = emailSenderService.sendEmail(
                        currentContact.getEmailAddress(),
                        subject,
                        body,
                        attachments
                );
                if (sendEmailResponseOptional.isPresent()) {
                    SendEmailResponse sendEmailResponse = sendEmailResponseOptional.get();
                    TaskStatus status = sendEmailResponse.getStatus();
                    log.info("Email send status is: ".concat(status.toString()));
                    currentContact.setStatus(EmailCommunicationContactStatus.fromClientStatus(status));
                } else {
                    currentContact.setStatus(EmailCommunicationContactStatus.ERROR);
                }
                emailCommunicationCustomerContactRepository.saveAndFlush(currentContact);
            }

            boolean isAnySend = emailCommunicationCustomerContacts.stream().anyMatch(contact -> EmailCommunicationContactStatus.SUCCESS.equals(contact.getStatus()));
            if (isAnySend) {
                emailCommunicationCustomer.setStatus(EmailCommunicationCustomerStatus.SENT_SUCCESSFULLY);
            } else {
                emailCommunicationCustomer.setStatus(EmailCommunicationCustomerStatus.IN_PROGRESS);
            }

            emailCommunicationCustomerRepository.saveAndFlush(emailCommunicationCustomer);
        }

    }


    /**
     * Sends a single email communication based on the provided {@code emailCommunicationId}.
     * This method performs the following steps:
     * Retrieves the {@link EmailCommunication} entity with the specified ID, checking that it is active.
     * Finds the associated {@link EmailCommunicationCustomer} for the email communication.
     * Fetches the attachments associated with the email communication.
     * For each contact associated with the email communication customer, attempts to send the email using the {@link EmailSenderService}.
     * Updates the status of each contact based on the email sending response.
     * Updates the status of the {@link EmailCommunicationCustomer} based on whether any emails were successfully sent.
     *
     * @param emailCommunicationId the ID of the {@link EmailCommunication} to be sent.
     * @see EmailCommunication
     * @see EmailCommunicationCustomer
     * @see EmailCommunicationCustomerContact
     * @see EmailSenderService
     * @see Attachment
     * @see EmailCommunicationContactStatus
     * @see EmailCommunicationCustomerStatus
     */
    @Async
    public void sendSingle(Long emailCommunicationId) {
        Optional<EmailCommunication> emailCommunicationOptional = emailCommunicationRepository.findByIdAndEntityStatus(emailCommunicationId, EntityStatus.ACTIVE);
        if (emailCommunicationOptional.isPresent()) {
            EmailCommunication emailCommunication = emailCommunicationOptional.get();
            Optional<EmailCommunicationCustomer> customerOptional = emailCommunicationCustomerRepository.findByEmailCommunicationId(emailCommunicationId);
            if (customerOptional.isPresent()) {
                List<Attachment> attachments = fetchAttachmentsForEmail(emailCommunicationId);
                String subject = emailCommunication.getEmailSubject();
                String body = emailCommunication.getEmailBody();
                EmailCommunicationCustomer emailCommunicationCustomer = customerOptional.get();
                List<EmailCommunicationCustomerContact> contacts = emailCommunicationCustomerContactRepository.findAllByEmailCommunicationCustomerId(emailCommunicationCustomer.getId());
                for (EmailCommunicationCustomerContact contact : contacts) {
                    Optional<SendEmailResponse> sendEmailResponseOptional = emailSenderService.sendEmail(
                            contact.getEmailAddress(),
                            subject,
                            body,
                            attachments
                    );
                    if (sendEmailResponseOptional.isPresent()) {
                        SendEmailResponse sendEmailResponse = sendEmailResponseOptional.get();
                        TaskStatus status = sendEmailResponse.getStatus();
                        log.info("Email send status is: ".concat(status.toString()));
                        contact.setStatus(EmailCommunicationContactStatus.fromClientStatus(status));
                        contact.setTaskId(sendEmailResponse.getTaskId().toString());
                    } else {
                        contact.setStatus(EmailCommunicationContactStatus.ERROR);
                    }
                    emailCommunicationCustomerContactRepository.save(contact);
                }
                boolean isAnySend = contacts.stream().anyMatch(contact -> EmailCommunicationContactStatus.SUCCESS.equals(contact.getStatus()));
                if (isAnySend) {
                    emailCommunicationCustomer.setStatus(EmailCommunicationCustomerStatus.SENT_SUCCESSFULLY);
                } else {
                    emailCommunicationCustomer.setStatus(EmailCommunicationCustomerStatus.IN_PROGRESS);
                }
                emailCommunicationCustomerRepository.saveAndFlush(emailCommunicationCustomer);
            }
        }
    }


    private List<Attachment> fetchAttachmentsForEmail(Long emailCommunicationId) {
        return emailCommunicationAttachmentRepository
                .findAllByEmailCommunicationIdAndStatus(emailCommunicationId)
                .stream()
                .map(f -> {
                    String fileName = f.substring(f.lastIndexOf('/') + 1);
                    return Pair.of(fileService.downloadFile(f), fileName);
                })
                .map(file -> this.createAttachmentForEmail(file.getKey(), file.getValue()))
                .toList();
    }

    private Attachment createAttachmentForEmail(ByteArrayResource byteArrayResource, String fileName) {
        return new Attachment(
                fileName,
                URLConnection.guessContentTypeFromName(fileName),
                byteArrayResource.getByteArray().length,
                byteArrayResource.getByteArray()
        );
    }

}
