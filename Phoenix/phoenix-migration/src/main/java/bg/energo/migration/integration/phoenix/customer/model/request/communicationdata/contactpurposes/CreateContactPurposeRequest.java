package bg.energo.migration.integration.phoenix.customer.model.request.communicationdata.contactpurposes;

import bg.energo.migration.integration.phoenix.customer.model.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateContactPurposeRequest {
    private Long contactPurposeId;
    private Status status;
}
