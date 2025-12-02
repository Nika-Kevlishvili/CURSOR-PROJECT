package bg.energo.phoenix.model.enums.template;

import bg.energo.phoenix.model.enums.documents.DocumentSigners;
import lombok.Getter;

@Getter
public enum ContractTemplateSigning {
    NO(DocumentSigners.NO),
    SIGNING_WITH_SYSTEM_CERTIFICATE(DocumentSigners.SYSTEM_CERTIFICATE),
    SIGNING_WITH_TABLET(DocumentSigners.SIGNATUS),
    SIGNING_WITH_QUALIFIED_SIGNATURE(DocumentSigners.QES);

    private final DocumentSigners documentSigners;

    ContractTemplateSigning(DocumentSigners documentSigners) {
        this.documentSigners = documentSigners;
    }
}
