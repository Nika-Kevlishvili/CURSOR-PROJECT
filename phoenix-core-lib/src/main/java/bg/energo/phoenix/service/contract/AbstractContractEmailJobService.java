package bg.energo.phoenix.service.contract;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.crm.emailCommunication.EmailCommunicationAttachment;
import bg.energo.phoenix.model.request.crm.emailCommunication.DocumentEmailCommunicationCreateRequest;
import bg.energo.phoenix.model.response.contract.ContractDocumentEmailResponse;
import bg.energo.phoenix.repository.crm.emailCommunication.EmailCommunicationAttachmentRepository;
import bg.energo.phoenix.service.crm.emailCommunication.EmailCommunicationService;
import bg.energo.phoenix.service.document.DocumentParserService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractContractEmailJobService {

    private final EmailCommunicationAttachmentRepository emailCommunicationAttachmentRepository;
    private final Long contractConclusionTopicId;
    private final EmailCommunicationService emailCommunicationService;

    protected abstract List<ContractDocumentEmailResponse> fetchDocumentsToSendEmail();

    protected abstract void updateDocumentsForSentEmails(Set<Long> documentIds);

    protected abstract ByteArrayResource generateEmailDocument(ContractDocumentEmailResponse response);

    @Transactional
    public List<String> sendEmailsForAllSignedDocuments() {
        List<ContractDocumentEmailResponse> emailResponses = fetchDocumentsToSendEmail();
        List<String> results = new ArrayList<>();
        Set<Long> documentsToUpdate = new HashSet<>();
        emailResponses
                .forEach(emailResponse -> {
                    try {
                        MDC.put("contractEmailId", emailResponse.getContractId().toString());
                        log.debug("start creating email for contract id {}, version {}", emailResponse.getContractId(), emailResponse.getContractVersion());
                        results.add(createEmailModel(emailResponse));
                        log.debug("created email for contract id {}, version {}", emailResponse.getContractId(), emailResponse.getContractVersion());
                        documentsToUpdate.addAll(
                                Arrays.stream(emailResponse.getContractDocumentIds().split(" "))
                                        .map(Long::parseLong)
                                        .collect(Collectors.toSet()));
                    } catch (Exception e) {
                        log.error("Could not send email for contract with id: {}, version : {}, error: {}", emailResponse.getContractId(), emailResponse.getContractVersion(), e.getMessage());
                        results.add("could not send email for contract with id: %s, version : %s, error : %s".formatted(emailResponse.getContractId(), emailResponse.getContractVersion(), e.getMessage()));
                    }
                });

        updateDocumentsForSentEmails(documentsToUpdate);

        return results;
    }

    //todo for test purposes, remove after
    @Transactional
    public String sendEmailForSignedDocumentsForContract(Long id, Long version) {
        List<ContractDocumentEmailResponse> emailResponses = fetchDocumentsToSendEmail();
        ContractDocumentEmailResponse emailResponse = emailResponses.stream().filter(e -> e.getContractId().equals(id) && e.getContractVersion().equals(version)).findFirst()
                .orElseThrow(() -> new ClientException("contract with id %s has invalid status or not all documents are signed".formatted(id), ErrorCode.UNSUPPORTED_OPERATION));
        String results;
        try {
            MDC.put("contractEmailId", emailResponse.getContractId().toString());
            log.debug("start creating email for contract id {}, version {}", emailResponse.getContractId(), emailResponse.getContractVersion());
            results = createEmailModel(emailResponse);
            log.debug("created email for contract id {}, version {}", emailResponse.getContractId(), emailResponse.getContractVersion());

        } catch (Exception e) {
            log.error("Could not send email for contract with id: {}, version : {}, error: {}", emailResponse.getContractId(), emailResponse.getContractVersion(), e.getMessage());
            results = "could not send email for contract with id: %s, version : %s, error : %s".formatted(emailResponse.getContractId(), emailResponse.getContractVersion(), e.getMessage());
        }

        updateDocumentsForSentEmails(Arrays.stream(emailResponse.getContractDocumentIds().split(" "))
                .map(Long::parseLong)
                .collect(Collectors.toSet()));

        return results;
    }

    protected String createEmailModel(ContractDocumentEmailResponse contract) {
        return "email sent for contract id: %s, version: %s, Email Communication id: %s"
                .formatted(contract.getContractId(),
                        contract.getContractVersion(),
                        emailCommunicationService.createEmailFromDocument(
                                DocumentEmailCommunicationCreateRequest
                                        .builder()
                                        .emailBoxId(contract.getMailboxId())
                                        .emailBody(generateEmailBody(contract))
                                        .emailSubject(contract.getEmailSubject())
                                        .emailTemplateId(contract.getEmailTemplateId())
                                        .attachmentFileIds(createAndSaveAttachments(contract.getDocumentUrls(), contract.getDocumentNames()))
                                        .communicationTopicId(contractConclusionTopicId)
                                        .customerCommunicationId(contract.getCustomerCommunicationId())
                                        .customerDetailId(contract.getCustomerDetailId())
                                        .customerEmailAddress(contract.getEmails())
                                        .build(), false));
    }

    protected Set<Long> createAndSaveAttachments(String documentUrls, String documentNames) {
        String[] urls = documentUrls.split("&");
        String[] names = documentNames.split("&");
        List<EmailCommunicationAttachment> attachments = new ArrayList<>();

        for (int i = 0; i < urls.length; i++) {
            EmailCommunicationAttachment attachment = EmailCommunicationAttachment.builder()
                    .status(EntityStatus.ACTIVE)
                    .name(names[i])
                    .fileUrl(urls[i])
                    .build();
            attachments.add(attachment);
        }
        return emailCommunicationAttachmentRepository.saveAllAndFlush(attachments)
                .stream().map(EmailCommunicationAttachment::getId).collect(Collectors.toSet());
    }

    @SneakyThrows
    protected String generateEmailBody(ContractDocumentEmailResponse response) {
        ByteArrayResource documentContent = generateEmailDocument(response);
        return DocumentParserService.parseDocxToHtml(documentContent.getContentAsByteArray());
    }

}
