package bg.energo.phoenix.service.contract.service;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.response.contract.ContractDocumentEmailResponse;
import bg.energo.phoenix.repository.contract.service.ServiceContractSignableDocumentsRepository;
import bg.energo.phoenix.repository.crm.emailCommunication.EmailCommunicationAttachmentRepository;
import bg.energo.phoenix.service.contract.AbstractContractEmailJobService;
import bg.energo.phoenix.service.crm.emailCommunication.EmailCommunicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service("serviceContractEmailJobService")
@Slf4j
public class ServiceContractEmailJobService extends AbstractContractEmailJobService {

    private final ServiceContractSignableDocumentsRepository signableDocumentsRepository;
    private final ServiceContractDocumentCreationService documentCreationService;

    public ServiceContractEmailJobService(
            EmailCommunicationAttachmentRepository emailCommunicationAttachmentRepository,
            EmailCommunicationService emailCommunicationService,
            @Value("${nomenclature.contract-conclusion.communication-topic.id}") Long contractConclusionTopicId,
            ServiceContractSignableDocumentsRepository signableDocumentsRepository,
            ServiceContractDocumentCreationService documentCreationService) {

        super(emailCommunicationAttachmentRepository, contractConclusionTopicId, emailCommunicationService);

        this.signableDocumentsRepository = signableDocumentsRepository;
        this.documentCreationService = documentCreationService;
    }

    @Override
    protected List<ContractDocumentEmailResponse> fetchDocumentsToSendEmail() {
        return signableDocumentsRepository.fetchContractAndDocumentsToSendEmail();
    }

    @Override
    protected void updateDocumentsForSentEmails(Set<Long> documentIds) {
        signableDocumentsRepository.updateDocumentsForSentEmails(documentIds);
    }

    @Override
    protected ByteArrayResource generateEmailDocument(ContractDocumentEmailResponse response) {
        return documentCreationService
                .generateEmailDocument(response.getContractId(), response.getContractVersion(), response.getEmailTemplateId())
                .orElseThrow(() -> new ClientException("Exception handled while trying to generate email document", ErrorCode.APPLICATION_ERROR));
    }
}