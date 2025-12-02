package bg.energo.phoenix.model.response.contract.serviceContract;

import bg.energo.phoenix.model.request.contract.relatedEntities.EntityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServiceContractRelatedFieldsResponse {

    private Long id;
    private Long contractId;
    private Long relatedContractId;
    private EntityType relatedContractType;

}
