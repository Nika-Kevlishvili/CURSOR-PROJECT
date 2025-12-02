package bg.energo.phoenix.model.request.customer.communicationData.contactPurpose;

import bg.energo.phoenix.model.enums.customer.Status;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BaseContactPurposeRequest {

    @NotNull(message = "communicationData.contactPurposes.contactPurposeId-Contact purpose id must not be null;")
    private Long contactPurposeId;

    @NotNull(message = "communicationData.contactPurposes.status-communication contact purpose request: status must not be null;")
    private Status status;

}
