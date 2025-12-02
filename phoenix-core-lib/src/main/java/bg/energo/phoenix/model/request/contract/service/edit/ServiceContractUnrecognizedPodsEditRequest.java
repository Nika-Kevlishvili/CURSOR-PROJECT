package bg.energo.phoenix.model.request.contract.service.edit;

import lombok.Data;

@Data
public class ServiceContractUnrecognizedPodsEditRequest {
    private Long id;
    private String podName;
}
