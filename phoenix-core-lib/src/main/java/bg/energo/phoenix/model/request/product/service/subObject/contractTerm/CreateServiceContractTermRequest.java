package bg.energo.phoenix.model.request.product.service.subObject.contractTerm;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CreateServiceContractTermRequest extends BaseServiceContractTermRequest {

    public CreateServiceContractTermRequest(EditServiceContractTermRequest editServiceContractTermRequest) {
        super(
                editServiceContractTermRequest.getName(),
                editServiceContractTermRequest.getPerpetuityCause(),
                editServiceContractTermRequest.getPeriodType(),
                editServiceContractTermRequest.getTermType(),
                editServiceContractTermRequest.getValue(),
                editServiceContractTermRequest.getAutomaticRenewal(),
                editServiceContractTermRequest.getNumberOfRenewals(),
                editServiceContractTermRequest.getRenewalPeriodValue(),
                editServiceContractTermRequest.getRenewalPeriodType()
        );
    }

}
