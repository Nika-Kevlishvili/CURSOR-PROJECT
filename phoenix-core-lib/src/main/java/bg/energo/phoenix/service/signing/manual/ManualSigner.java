package bg.energo.phoenix.service.signing.manual;

import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.enums.documents.DocumentSigners;
import bg.energo.phoenix.model.enums.documents.DocumentStatus;
import bg.energo.phoenix.repository.documents.DocumentsRepository;
import bg.energo.phoenix.service.archivation.edms.SignedDocumentFileArchivation;
import bg.energo.phoenix.service.signing.SignerChain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ManualSigner implements SignerChain {
    private final DocumentsRepository documentsRepository;
    private final SignedDocumentFileArchivation signedDocumentFileArchivation;


    @Override
    public DocumentSigners getSigner() {
        return DocumentSigners.NO;
    }

    @Override
    public List<Document> sign(List<Document> documents) {
        for (Document document : documents) {
            document.setDocumentStatus(DocumentStatus.SIGNED);
            signedDocumentFileArchivation.archiveSignedFile(document);
            documentsRepository.save(document);
        }
        return new ArrayList<>();
    }
}
