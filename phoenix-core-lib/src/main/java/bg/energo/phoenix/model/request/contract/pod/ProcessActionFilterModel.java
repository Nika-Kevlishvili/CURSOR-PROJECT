package bg.energo.phoenix.model.request.contract.pod;

import bg.energo.phoenix.model.entity.contract.billing.ContractPods;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter

public class ProcessActionFilterModel extends ActionFilterModel {
    private Long processInfoId;

    public ProcessActionFilterModel(Long detailId, Long contractId, LocalDate startDate, ContractPods contractPods, Long customerDetailId, Long podId, Long customerId, Long processInfoId) {
        super(detailId, contractId, startDate, contractPods, customerDetailId, podId, customerId);
        this.processInfoId = processInfoId;
    }
}
