package bg.energo.phoenix.model.response.contract.InterestRate;

import bg.energo.phoenix.model.entity.contract.InterestRate.InterestRate;
import bg.energo.phoenix.model.enums.contract.InterestRate.InterestRateStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InterestRateShortResponse {
    private Long id;
    private String name;
    private InterestRateStatus status;
    private Boolean isDefault;

    public InterestRateShortResponse(InterestRate interestRate) {
        this.id = interestRate.getId();
        this.name = interestRate.getName();
        this.status = interestRate.getStatus();
        this.isDefault = interestRate.getIsDefault();
    }
}
