package bg.energo.phoenix.service.signing.qes;

import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.entity.template.ContractTemplateDetail;
import bg.energo.phoenix.model.entity.template.QesDocument;
import bg.energo.phoenix.model.enums.documents.DocumentSigners;
import bg.energo.phoenix.model.enums.template.QesSigningStatus;
import bg.energo.phoenix.model.enums.template.QesStatus;
import bg.energo.phoenix.repository.template.ContractTemplateDetailsRepository;
import bg.energo.phoenix.repository.template.QesDocumentRepository;
import bg.energo.phoenix.service.archivation.edms.FileArchivationService;
import bg.energo.phoenix.service.signing.SignerChain;
import bg.energo.phoenix.service.signing.qes.entities.QesDocumentDetails;
import bg.energo.phoenix.service.signing.qes.entities.QesDocumentDetailsStatus;
import bg.energo.phoenix.service.signing.qes.repositories.QesDocumentDetailsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QesChain implements SignerChain {
    private final ContractTemplateDetailsRepository templateRepository;
    private final QesDocumentDetailsRepository qesDocumentDetailsRepository;
    private final QesDocumentRepository qesDocumentRepository;
    private final FileArchivationService fileArchivationService;

    @Override
    public List<Document> sign(List<Document> documents) {
        Set<ContractTemplateDetail> templateDetails = templateRepository.findRespectiveTemplateDetailsByTemplateIds(documents.stream().map(Document::getTemplateId).collect(Collectors.toSet()));
        Map<Long, Integer> quantityMap = templateDetails.stream().collect(Collectors.toMap(ContractTemplateDetail::getTemplateId, ContractTemplateDetail::getQuantity));
        setupDocs(documents, quantityMap);
        return new ArrayList<>();
    }

    @Override
    public DocumentSigners getSigner() {
        return DocumentSigners.QES;
    }

    public void setupDocs(List<Document> documents, Map<Long, Integer> signCounts) {
        for (Document document : documents) {
            QesDocument qesDocument = new QesDocument();
            qesDocument.setDocument_id(document.getId());
            qesDocument.setIdentifier(UUID.randomUUID().toString());
            qesDocument.setSignedQuantity(0);
            qesDocument.setQuantityToSign(signCounts.get(document.getTemplateId()));
            qesDocument.setSigningStatus(QesSigningStatus.TO_BE_SIGNED);
            qesDocument.setStatus(QesStatus.ACTIVE);
            QesDocument save = qesDocumentRepository.save(qesDocument);
            QesDocumentDetails qesDocumentDetails = new QesDocumentDetails(null, save.getId(), null, QesDocumentDetailsStatus.SIGNED, document.getSignedFileUrl(), true);
            qesDocumentDetailsRepository.save(qesDocumentDetails);
        }
    }
}
