package bg.energo.phoenix.service.contract.newVersionEvent.serviceContract;

import bg.energo.phoenix.model.entity.product.service.ServiceDetails;
import bg.energo.phoenix.model.entity.product.term.terms.Terms;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServiceContractCreateNewVersionEvent {
    private ServiceDetails currentServiceDetails;
    private List<String> exceptionMessagesContext;
    private Terms productContractValidTerm;
    private Long serviceRelatedContractId;
    private Long serviceRelatedContractVersion;
    private Long serviceRelatedContractCustomerDetailId;

}
