package bg.energo.phoenix.model.request.contract.pod;

import bg.energo.phoenix.model.entity.contract.billing.ContractPods;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActivationFilteredModel {
    private Long detailId;
    private Long contractId;
    private String contractNumber;
    private LocalDate startDate;
    private ContractPods contractPods;
    private Integer versionId;

    public ActivationFilteredModel(Long detailId, Long contractId, LocalDate startDate, ContractPods contractPods) {
        this.detailId = detailId;
        this.contractId = contractId;
        this.startDate = startDate;
        this.contractPods = contractPods;
    }
}
