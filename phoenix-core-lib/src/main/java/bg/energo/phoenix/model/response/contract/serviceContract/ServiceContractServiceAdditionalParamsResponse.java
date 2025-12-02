package bg.energo.phoenix.model.response.contract.serviceContract;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ServiceContractServiceAdditionalParamsResponse {
    private Long serviceAdditionalParamsId;
    private String value;
}
