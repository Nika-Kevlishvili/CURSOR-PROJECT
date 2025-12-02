package bg.energo.phoenix.model.request.contract.pod;

import bg.energo.phoenix.model.entity.contract.billing.ContractPods;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ActionFilterModel extends ActivationFilteredModel {
    private Long customerDetailId;
    private Long podId;
    private Long customerId;

    public ActionFilterModel(Long detailId,
                             Long contractId,
                             LocalDate startDate,
                             ContractPods contractPods,
                             Long customerDetailId,
                             Long podId,
                             Long customerId) {
        super(detailId, contractId, startDate, contractPods);
        this.customerDetailId = customerDetailId;
        this.podId = podId;
        this.customerId = customerId;
    }

    public ActionFilterModel(Long detailId,
                             Long contractId,
                             String contractNumber,
                             Integer versionId,
                             LocalDate startDate,
                             ContractPods contractPods,
                             Long customerDetailId,
                             Long podId,
                             Long customerId) {
        super(detailId, contractId, contractNumber, startDate, contractPods, versionId);
        this.customerDetailId = customerDetailId;
        this.podId = podId;
        this.customerId = customerId;
    }
}
