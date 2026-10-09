package bg.energo.migration.integration.phoenix.customer.model.request.owner;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerOwnerRequest {
    private String personalNumber;
    private String additionalInformation;
    private Long belongingOwnerCapitalId;
}
