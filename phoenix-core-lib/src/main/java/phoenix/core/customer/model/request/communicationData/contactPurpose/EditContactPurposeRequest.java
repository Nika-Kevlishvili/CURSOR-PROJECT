package phoenix.core.customer.model.request.communicationData.contactPurpose;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EditContactPurposeRequest extends BaseContactPurposeRequest {

    private Long id;

}
