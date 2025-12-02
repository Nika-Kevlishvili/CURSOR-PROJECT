package bg.energo.phoenix.model.response.contract.express;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerExpressContractDto {
    private Long customerId;
    private Long customerDetailId;
    private Long version;
    private String identifier;
}
