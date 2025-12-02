package bg.energo.phoenix.model.response.contract.serviceContract;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServiceContractContractVersionTypesResponse {
    private Long serviceContractVersionId;
    private Long id;
    private String name;
}
