package bg.energo.phoenix.model.request.contract.pod;


import bg.energo.phoenix.model.entity.contract.billing.ContractPods;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class DeactivationFilteredModel extends ActivationFilteredModel {
    private String contractNumber;
    private Integer versionId;

    public DeactivationFilteredModel(Long detailId, Long contractId, LocalDate startDate, ContractPods contractPods, String contractNumber, Integer versionId) {
        super(detailId, contractId, startDate, contractPods);
        this.contractNumber = contractNumber;
        this.versionId = versionId;
    }
}
