package bg.energo.phoenix.model.response.contract.InterestRate;

import bg.energo.phoenix.model.enums.contract.InterestRate.InterestRateCharging;
import bg.energo.phoenix.model.enums.contract.InterestRate.InterestRateStatus;
import bg.energo.phoenix.model.enums.contract.InterestRate.InterestRateType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class InterestRateListResponse {
    private Long id;
    private String name;
    private InterestRateCharging charging;
    private InterestRateType type;
    private Boolean grouping;
    private Boolean isDefault;
    private LocalDateTime createDate;
    private InterestRateStatus status;

    public InterestRateListResponse(Long id, String name, InterestRateCharging charging, InterestRateType type, Boolean grouping, Boolean isDefault, LocalDateTime createDate, InterestRateStatus status) {
        this.id = id;
        this.name = name;
        this.charging = charging;
        this.type = type;
        this.grouping = grouping;
        this.isDefault = isDefault;
        this.createDate = createDate;
        this.status = status;
    }
}
