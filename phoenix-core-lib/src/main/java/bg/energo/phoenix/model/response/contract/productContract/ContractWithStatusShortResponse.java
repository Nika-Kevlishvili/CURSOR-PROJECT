package bg.energo.phoenix.model.response.contract.productContract;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ContractWithStatusShortResponse {

    private Long id;

    private String status;

    private String subStatus;

}
