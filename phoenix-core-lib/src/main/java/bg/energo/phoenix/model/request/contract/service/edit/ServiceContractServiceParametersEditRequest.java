package bg.energo.phoenix.model.request.contract.service.edit;


import bg.energo.phoenix.model.request.contract.service.ServiceContractServiceParametersCreateRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;


@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServiceContractServiceParametersEditRequest extends ServiceContractServiceParametersCreateRequest {
    private List<@Valid ServiceContractContractNumbersEditRequest> contractNumbersEditList;
    private List<@Valid ServiceContractPodsEditRequest> podsEditList;
    private List<@Valid ServiceContractUnrecognizedPodsEditRequest> unrecognizedPodsEditList;
}
