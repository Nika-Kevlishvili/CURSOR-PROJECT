package bg.energo.phoenix.service.signing.qes;

import bg.energo.phoenix.model.entity.template.QesDocument;
import bg.energo.phoenix.service.signing.qes.entities.QesDocumentDetails;

public interface QesDocumentDto {
    QesDocumentDetails getDetails();
    QesDocument getQes();

}