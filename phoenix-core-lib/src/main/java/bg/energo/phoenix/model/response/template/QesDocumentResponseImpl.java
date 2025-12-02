package bg.energo.phoenix.model.response.template;

import bg.energo.phoenix.model.enums.customer.CustomerType;
import bg.energo.phoenix.model.enums.template.ContractTemplatePurposes;
import bg.energo.phoenix.model.enums.template.QesSigningStatus;
import bg.energo.phoenix.model.enums.template.QesStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class QesDocumentResponseImpl implements QesDocumentResponse {

    private Long documentId;
    private String fileName;
    private String fileIdentifier;
    private CustomerType customerType;
    private String customerName;
    private String middleName;
    private String lastName;
    private String customerIdentifier;
    private String legalFormName;
    private ContractTemplatePurposes templatePurpose;
    private String salesChannel;
    private Integer quantityToSign;
    private Integer signedQuantity;
    private QesStatus status;
    private QesSigningStatus signingStatus;
    private LocalDateTime updateTime;
    private String type;


    public QesDocumentResponseImpl(QesDocumentResponse qesDocumentResponse) {
        this.documentId = qesDocumentResponse.getDocumentId();
        this.fileName = qesDocumentResponse.getFileName();
        this.fileIdentifier = qesDocumentResponse.getFileIdentifier();

        this.customerType = qesDocumentResponse.getCustomerType();
        this.customerName = qesDocumentResponse.getCustomerName();
        this.middleName = qesDocumentResponse.getMiddleName();
        this.lastName = qesDocumentResponse.getLastName();
        this.customerIdentifier = qesDocumentResponse.getCustomerIdentifier();
        this.legalFormName = qesDocumentResponse.getLegalFormName();
        this.templatePurpose = qesDocumentResponse.getTemplatePurpose();
        this.salesChannel = qesDocumentResponse.getSalesChannel();
        this.quantityToSign = qesDocumentResponse.getQuantityToSign();
        this.signedQuantity = qesDocumentResponse.getSignedQuantity();
        this.status = qesDocumentResponse.getStatus();
        this.signingStatus = qesDocumentResponse.getSigningStatus();
        this.updateTime = qesDocumentResponse.getUpdateTime();
        this.type = qesDocumentResponse.getType();
    }
}
