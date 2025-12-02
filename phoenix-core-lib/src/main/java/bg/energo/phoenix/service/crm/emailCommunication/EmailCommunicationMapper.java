package bg.energo.phoenix.service.crm.emailCommunication;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.crm.emailCommunication.*;
import bg.energo.phoenix.model.enums.crm.emailCommunication.EmailCommunicationChannelType;
import bg.energo.phoenix.model.enums.crm.emailCommunication.EmailCommunicationDocGenerationStatus;
import bg.energo.phoenix.model.enums.crm.emailCommunication.EmailCommunicationStatus;
import bg.energo.phoenix.model.enums.crm.emailCommunication.EmailCommunicationType;
import bg.energo.phoenix.model.enums.receivable.CreationType;
import bg.energo.phoenix.model.enums.shared.DocumentFileStatus;
import bg.energo.phoenix.model.request.crm.emailCommunication.*;
import bg.energo.phoenix.model.response.crm.emailCommunication.EmailCommunicationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailCommunicationMapper {
    public EmailCommunication fromCreateRequestToEntity(EmailCommunicationCreateRequest request) {
        return EmailCommunication
                .builder()
                .sentDate(EmailCommunicationType.INCOMING.equals(request.getEmailCommunicationType()) ? request.getDateTime() : null)
                .communicationChannel(EmailCommunicationChannelType.EMAIL)
                .communicationAsAnInstitution(request.getCommunicationAsAnInstitution())
                .dmsNumber(request.getDmsNumber())
                .emailCommunicationType(request.getEmailCommunicationType())
                .emailSubject(request.getEmailSubject())
                .emailBody(request.getEmailBody())
                .entityStatus(EntityStatus.ACTIVE)
                .creationType(CreationType.MANUAL)
                .build();
    }

    public EmailCommunication fromDocumentRequestToEntity(DocumentEmailCommunicationCreateRequest request) {
        return EmailCommunication
                .builder()
                .sentDate(LocalDateTime.now())
                .communicationChannel(EmailCommunicationChannelType.EMAIL)
                .communicationAsAnInstitution(false)
                .emailCommunicationType(EmailCommunicationType.OUTGOING)
                .emailSubject(request.getEmailSubject())
                .emailBody(request.getEmailBody())
                .emailTemplateId(request.getEmailTemplateId())
                .entityStatus(EntityStatus.ACTIVE)
                .emailCommunicationStatus(EmailCommunicationStatus.SENT)
                .creationType(CreationType.AUTOMATIC)
                .build();
    }

    public EmailCommunication fromMassEmailCreateRequestToEntity(MassEmailCreateRequest request) {
        return EmailCommunication
                .builder()
                .communicationAsAnInstitution(request.getCommunicationAsInstitution())
                .communicationChannel(EmailCommunicationChannelType.MASS_EMAIL)
                .emailCommunicationType(EmailCommunicationType.OUTGOING)
                .emailSubject(request.getSubject())
                .emailBody(request.getEmailBody())
                .entityStatus(EntityStatus.ACTIVE)
                .creationType(CreationType.MANUAL)
                .docGenerationStatus(EmailCommunicationDocGenerationStatus.READY)
                .build();
    }

    public EmailCommunicationFile emailCommunicationFile(String fileUrl,
                                                         String name,
                                                         Boolean isReport,
                                                         List<DocumentFileStatus> statuses
    ) {
        return EmailCommunicationFile
                .builder()
                .status(EntityStatus.ACTIVE)
                .isReport(isReport)
                .localFileUrl(fileUrl)
                .name(name)
                .fileStatuses(statuses)
                .build();
    }

    public EmailCommunicationAttachment emailCommunicationAttachment(String fileUrl, String name) {
        return EmailCommunicationAttachment
                .builder()
                .status(EntityStatus.ACTIVE)
                .fileUrl(fileUrl)
                .name(name)
                .build();
    }

    public EmailCommunicationCustomer emailCommunicationCustomer(Long emailCommunicationId, Long customerDetailId, Long customerCommunicationId) {
        return EmailCommunicationCustomer
                .builder()
                .customerCommunicationId(customerCommunicationId)
                .emailCommunicationId(emailCommunicationId)
                .customerDetailId(customerDetailId)
                .build();
    }

    public EmailCommunicationCustomer emailCommunicationCustomer(Long emailCommunicationId,
                                                                 Long customerDetailId,
                                                                 Long customerCommunicationId,
                                                                 Long contactPurposeId
    ) {
        EmailCommunicationCustomer emailCommunicationCustomer = emailCommunicationCustomer(
                emailCommunicationId,
                customerDetailId,
                customerCommunicationId
        );
        emailCommunicationCustomer.setContactPurposeId(contactPurposeId);
        return emailCommunicationCustomer;
    }

    public EmailCommunicationCustomerContact emailCommunicationCustomerContact(Long customerCommunicationContactId,
                                                                               Long emailCommunicationCustomerId,
                                                                               String emailAddress
    ) {
        return EmailCommunicationCustomerContact
                .builder()
                .customerCommunicationContactId(customerCommunicationContactId)
                .emailCommunicationCustomerId(emailCommunicationCustomerId)
                .emailAddress(emailAddress)
                .build();
    }

    public EmailCommunicationResponse fromEntityToPreviewResponse(EmailCommunication emailCommunication) {
        return EmailCommunicationResponse
                .builder()
                .communicationAsAnInstitution(emailCommunication.getCommunicationAsAnInstitution())
                .emailCommunicationStatus(emailCommunication.getEmailCommunicationStatus())
                .communicationChannelType(emailCommunication.getCommunicationChannel())
                .communicationType(emailCommunication.getEmailCommunicationType())
                .emailCreationType(emailCommunication.getCreationType())
                .emailSubject(emailCommunication.getEmailSubject())
                .entityStatus(emailCommunication.getEntityStatus())
                .dmsNumber(emailCommunication.getDmsNumber())
                .emailBody(emailCommunication.getEmailBody())
                .sentDate(emailCommunication.getSentDate())
                .id(emailCommunication.getId())
                .build();
    }

    public EmailCommunicationResponse fromMassEntityToSinglePreviewResponse(EmailCommunication emailCommunication) {
        return EmailCommunicationResponse
                .builder()
                .communicationAsAnInstitution(emailCommunication.getCommunicationAsAnInstitution())
                .emailCommunicationStatus(emailCommunication.getEmailCommunicationStatus())
                .communicationChannelType(EmailCommunicationChannelType.EMAIL)
                .communicationType(emailCommunication.getEmailCommunicationType())
                .emailSubject(emailCommunication.getEmailSubject())
                .entityStatus(emailCommunication.getEntityStatus())
                .sentDate(emailCommunication.getSentDate())
                .id(emailCommunication.getId())
                .build();
    }

    public EmailCommunicationRelatedCustomer emailCommunicationRelatedCustomer(Long emailCommunicationId, Long customerId) {
        return EmailCommunicationRelatedCustomer
                .builder()
                .emailCommunicationId(emailCommunicationId)
                .status(EntityStatus.ACTIVE)
                .customerId(customerId)
                .build();
    }

    public EmailCommunication fromEntityToResendEntity(EmailCommunication emailCommunication) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        LocalDateTime localDateTime = LocalDateTime.now();
        String additionalInfo = String.format(
                " This email was sent on %s.",
                localDateTime.format(formatter)
        );

        return EmailCommunication
                .builder()
                .dmsNumber(emailCommunication.getDmsNumber())
                .communicationAsAnInstitution(emailCommunication.getCommunicationAsAnInstitution())
                .emailCommunicationType(EmailCommunicationType.OUTGOING)
                .emailCommunicationStatus(EmailCommunicationStatus.SENT)
                .communicationChannel(EmailCommunicationChannelType.EMAIL)
                .emailTemplateId(emailCommunication.getEmailTemplateId())
                .emailSubject(emailCommunication.getEmailSubject())
                .emailBody(emailCommunication.getEmailBody() + additionalInfo)
                .entityStatus(EntityStatus.ACTIVE)
                .sentDate(localDateTime)
                .build();
    }

    public EmailCommunication fromEditRequestToEntity(EmailCommunication emailCommunication, EmailCommunicationEditRequest request) {
        emailCommunication.setSentDate(EmailCommunicationType.INCOMING.equals(request.getEmailCommunicationType()) ? request.getDateTime() : null);
        emailCommunication.setCommunicationAsAnInstitution(request.getCommunicationAsAnInstitution());
        emailCommunication.setEmailCommunicationType(request.getEmailCommunicationType());
        emailCommunication.setEmailSubject(request.getEmailSubject());
        emailCommunication.setDmsNumber(request.getDmsNumber());
        emailCommunication.setEmailBody(request.getEmailBody());
        return emailCommunication;
    }

    public EmailCommunication fromMassEditRequestToEntity(EmailCommunication emailCommunication, MassEmailEditRequest request) {
        emailCommunication.setCommunicationAsAnInstitution(request.getCommunicationAsInstitution());
        emailCommunication.setEmailCommunicationType(EmailCommunicationType.OUTGOING);
        emailCommunication.setEmailSubject(request.getSubject());
        emailCommunication.setEmailBody(request.getEmailBody());
        return emailCommunication;
    }

    public EmailCommunicationContactPurpose emailCommunicationContactPurpose(Long emailCommunicationId, Long contactPurposeId) {
        return EmailCommunicationContactPurpose
                .builder()
                .emailCommunicationId(emailCommunicationId)
                .contactPurposeId(contactPurposeId)
                .status(EntityStatus.ACTIVE)
                .build();
    }

}
