package bg.energo.phoenix.model.response.receivable.manualLiabilityOffsetting;

import lombok.Data;

@Data
public class ManualLiabilityOffsettingDepositInfo{
    private Long id;
    private Long liabilityId;

    public ManualLiabilityOffsettingDepositInfo(Long id, Long liabilityId) {
        this.id = id;
        this.liabilityId = liabilityId;
    }
}
