package phoenix.core.customer.model.request.communicationData.contactPurpose;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import phoenix.core.customer.model.enums.customer.Status;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BaseContactPurposeRequest {

    @NotNull
    private Long contactPurposeId;

    @NotNull
    private Status status;

}
