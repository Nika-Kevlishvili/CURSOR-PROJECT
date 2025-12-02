package bg.energo.phoenix.model.response.nomenclature.customer;

import bg.energo.phoenix.model.entity.nomenclature.customer.ContactPurpose;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import lombok.Data;

@Data
public class ContactPurposeResponse {
    private Long id;
    private String name;
    private Long orderingId;
    private boolean defaultSelection;
    private Boolean isHardCoded;
    private NomenclatureItemStatus status;
    private String systemUserId;


    public ContactPurposeResponse(ContactPurpose contactPurpose) {
        this.id = contactPurpose.getId();
        this.name = contactPurpose.getName();
        this.orderingId = contactPurpose.getOrderingId();
        this.defaultSelection = contactPurpose.isDefaultSelection();
        this.status = contactPurpose.getStatus();
        this.systemUserId = contactPurpose.getSystemUserId();
        this.isHardCoded = contactPurpose.getIsHardCoded();
    }
}
