package bg.energo.phoenix.model.request.product.service;

import bg.energo.phoenix.model.enums.product.product.EntityType;
import bg.energo.phoenix.model.enums.product.service.ServiceAllowsSalesUnder;
import bg.energo.phoenix.model.enums.product.service.ServiceObligationCondition;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ServiceRelatedEntityRequest {
    private Long id;
    private EntityType type;
    private ServiceObligationCondition obligatory;
    private ServiceAllowsSalesUnder allowSalesUnder;
}
