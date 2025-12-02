package bg.energo.phoenix.model.response.nomenclature.customer;

import bg.energo.phoenix.model.entity.nomenclature.customer.ContactPurpose;
import lombok.Data;

@Data
public class ContactPurposeShortResponse {
    private Long id;
    private String name;

    public ContactPurposeShortResponse(ContactPurpose contactPurpose) {
        this.id = contactPurpose.getId();
        this.name = contactPurpose.getName();
    }
}
