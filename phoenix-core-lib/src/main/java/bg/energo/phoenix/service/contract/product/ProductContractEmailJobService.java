package bg.energo.phoenix.service.contract.product;


import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.response.contract.ContractDocumentEmailResponse;
import bg.energo.phoenix.repository.contract.product.ProductContractSignableDocumentRepository;
import bg.energo.phoenix.repository.crm.emailCommunication.EmailCommunicationAttachmentRepository;
import bg.energo.phoenix.service.contract.AbstractContractEmailJobService;
import bg.energo.phoenix.service.crm.emailCommunication.EmailCommunicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service("productContractEmailJobService")
@Slf4j
public class ProductContractEmailJobService extends AbstractContractEmailJobService {

    private final ProductContractSignableDocumentRepository signableDocumentsRepository;
    private final ProductContractDocumentCreationService documentCreationService;

    public ProductContractEmailJobService(
            EmailCommunicationService emailCommunicationService,
            EmailCommunicationAttachmentRepository emailCommunicationAttachmentRepository,
            @Value("${nomenclature.contract-conclusion.communication-topic.id}") Long contractConclusionTopicId,
            ProductContractSignableDocumentRepository signableDocumentsRepository,
            ProductContractDocumentCreationService documentCreationService) {
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
                .generateEmailDocument(response.getContractId(), response.getContractVersion().intValue(), response.getEmailTemplateId())
                .orElseThrow(() -> new ClientException("Exception handled while trying to generate email document", ErrorCode.APPLICATION_ERROR));
    }
}

