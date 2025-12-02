package bg.energo.phoenix.model.response.contract.InterestRate;

import bg.energo.phoenix.model.enums.contract.InterestRate.InterestRateCharging;
import bg.energo.phoenix.model.enums.contract.InterestRate.InterestRatePeriodicity;
import bg.energo.phoenix.model.enums.contract.InterestRate.InterestRateStatus;
import bg.energo.phoenix.model.enums.contract.InterestRate.InterestRateType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InterestRateResponse {

    //FirstTab: General settings
    private Long id;
    private String name;
    private Boolean isDefault;
    private InterestRateType type;
    private InterestRateCharging charging;
    private BigDecimal minAmountForInterestCharging;
    private BigDecimal minAmountOfInterest;
    private BigDecimal maxAmountOfInterest;
    private Long currencyId;
    private String currencyName;
    private BigDecimal minAmountOfInterestInPercentOfLiability;
    private BigDecimal maxAmountOfInterestInPercentOfTheLiability;
    private Integer gracePeriod;
    private InterestRatePeriodicity periodicity;
    private Boolean grouping;
    private String incomeAccountNumber;
    private String costCenterControllingOrder;
    private InterestRateStatus status;

    //SecondTab: Rates And periods settings
    private List<InterestRatePeriodsResponse> interestRatePeriodsResponse;

    //SubObjects
    private InterestRatePaymentTermResponse paymentTermResponse;
}
