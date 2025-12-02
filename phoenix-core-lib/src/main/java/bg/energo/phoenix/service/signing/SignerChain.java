package bg.energo.phoenix.service.signing;

import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.enums.documents.DocumentSigners;
import org.apache.commons.lang3.NotImplementedException;

import java.util.List;

public interface SignerChain {

    List<Document> sign(List<Document> documents);

    default List<Document> process(List<Document> documentIds) {
        throw new NotImplementedException("Process is not Implemented");
    }

    DocumentSigners getSigner();
}
