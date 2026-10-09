package bg.energo.migration.integration.phoenix.customer.model.request.accountmanagers;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCustomerAccountManagerRequest {
    private Long accountManagerId;
    private Long accountManagerTypeId;
}
