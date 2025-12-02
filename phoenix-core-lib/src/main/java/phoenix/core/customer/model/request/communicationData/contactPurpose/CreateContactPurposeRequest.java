package phoenix.core.customer.model.request.communicationData.contactPurpose;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CreateContactPurposeRequest extends BaseContactPurposeRequest {

    public CreateContactPurposeRequest(EditContactPurposeRequest editContactPurposeRequest) {
        this.setContactPurposeId(editContactPurposeRequest.getContactPurposeId());
        this.setStatus(editContactPurposeRequest.getStatus());
    }

}
