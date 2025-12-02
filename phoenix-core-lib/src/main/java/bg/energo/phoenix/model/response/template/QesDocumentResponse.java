package bg.energo.phoenix.model.response.template;

import bg.energo.phoenix.model.enums.customer.CustomerType;
import bg.energo.phoenix.model.enums.template.ContractTemplatePurposes;
import bg.energo.phoenix.model.enums.template.QesSigningStatus;
import bg.energo.phoenix.model.enums.template.QesStatus;

import java.time.LocalDateTime;



public interface QesDocumentResponse {
     Long getDocumentId();
     String getFileName();
     String getFileIdentifier();
     CustomerType getCustomerType();
     String getCustomerName();
     String getMiddleName();
     String getLastName();
     String getCustomerIdentifier();
     String getLegalFormName();
     ContractTemplatePurposes getTemplatePurpose();
     String getSalesChannel();
     Integer getQuantityToSign();
     Integer getSignedQuantity();
     QesStatus getStatus();
     QesSigningStatus getSigningStatus();
     LocalDateTime getUpdateTime();
     String getType();

}
