package bg.energo.phoenix.model.response.contract.biling;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BillingGroupListingResponse {

    private Long id;
    private String groupNumber;
}
