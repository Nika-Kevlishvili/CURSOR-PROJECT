package bg.energo.phoenix.model.request.contract.interestRate;

import bg.energo.phoenix.model.customAnotations.contract.interestRate.InterestRatePeriodsCreateValidator;
import bg.energo.phoenix.model.customAnotations.contract.interestRate.InterestRateValidPeriodicityValidator;
import bg.energo.phoenix.model.enums.contract.InterestRate.InterestRateCharging;
import bg.energo.phoenix.model.enums.contract.InterestRate.InterestRatePeriodicity;
import bg.energo.phoenix.model.enums.contract.InterestRate.InterestRateType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Range;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@InterestRateValidPeriodicityValidator
@InterestRatePeriodsCreateValidator
public class InterestRateCreateRequest {

    //FirstTab: General settings
    @Size(min = 1, max = 512, message = "name-Order Number length is out of range: [{min}:{max}];")
    //@Pattern(regexp = "^[А-Яа-яA-Za-z\\d!#$%&'*+\\-/=?^_`|{}~.()\\s]*$", message = "name-[Name] has invalid pattern;")
    @NotBlank(message = "name-Must not be blank;")
    private String name;

    @NotNull(message = "isDefault-[IsDefault] Must not be null;")
    private Boolean isDefault;

    @NotNull(message = "type-[Interest Rate Type] Must not be null;")
    private InterestRateType type;

    @NotNull(message = "charging-[Interest Charging] Must not be null;")
    private InterestRateCharging charging;

    @DecimalMin(value = "0.01", message = "minAmountForInterestCharging-[MinAmountForInterestCharging] should be more or equal than 0.01;")
    @DecimalMax(value = "999999999999.99", message = "minAmountForInterestCharging-[MinAmountForInterestCharging] should be less or equal than 999999999999.99;")
    private BigDecimal minAmountForInterestCharging;

    @DecimalMin(value = "0.01", message = "minAmountOfInterest-[MinAmountOfInterest] should be more than 0.01;")
    @DecimalMax(value = "999999999999.99", message = "minAmountOfInterest-[MinAmountOfInterest] should be less than 999999999999.99;")
    private BigDecimal minAmountOfInterest;

    @DecimalMin(value = "0.01", message = "maxAmountOfInterest-[MaxAmountOfInterest] should be more than 0.01;")
    @DecimalMax(value = "999999999999.99", message = "maxAmountOfInterest-[MaxAmountOfInterest] should be less than 999999999999.99;")
    private BigDecimal maxAmountOfInterest;

    @NotNull(message = "currencyId-[CurrencyId] Must not be null;")
    private Long currencyId;

    @DecimalMin(value = "0.01", message = "minAmountOfInterestInPercentOfLiability-[MinAmountOfInterestInPercentOfLiability] should be more than 0;")
    @DecimalMax(value = "100", message = "minAmountOfInterestInPercentOfLiability-[MinAmountOfInterestInPercentOfLiability] should be less than 100;")
    @Digits(integer = 3, fraction = 2, message = "minAmountOfInterestInPercentOfLiability-[MinAmountOfInterestInPercentOfLiability] accepts max {integer} integral and max {fraction} fractional digits;")
    private BigDecimal minAmountOfInterestInPercentOfLiability;

    @DecimalMin(value = "0.01", message = "maxAmountOfInterestInPercentOfTheLiability-[MaxAmountOfInterestInPercentOfTheLiability] should be more than 0;")
    @DecimalMax(value = "100", message = "maxAmountOfInterestInPercentOfTheLiability-[MaxAmountOfInterestInPercentOfTheLiability] should be less than 100;")
    @Digits(integer = 3, fraction = 2, message = "maxAmountOfInterestInPercentOfTheLiability-[MaxAmountOfInterestInPercentOfTheLiability] accepts max {integer} integral and max {fraction} fractional digits;")
    private BigDecimal maxAmountOfInterestInPercentOfTheLiability;

    @Range(min = 1, max = 9999, message = "gracePeriod-[gracePeriod] size must be between 1_9999;")
    private Integer gracePeriod;

    private InterestRatePeriodicity periodicity;

    @NotNull(message = "grouping-[Grouping] Must not be null;")
    private Boolean grouping;

    @NotNull(message = "incomeAccountNumber-[IncomeAccountNumber] Must not be null;")
    private String incomeAccountNumber;

    @NotEmpty(message = "costCenterControllingOrder-[costCenterControllingOrder] Must not be empty;")
    @Size(min = 1, max = 32, message = "costCenterControllingOrder-[CostCenterControllingOrder] length is out of range: [{min}:{max}];")
    private String costCenterControllingOrder;
    //SecondTab: Rates And periods settings
    private List<@Valid InterestRatePeriodsCreateRequest> interestRatePeriods;
    //SubObjects
    @NotNull(message = "paymentTerm-[Payment Term] Must not be null;")
    private @Valid InterestRatePaymentTermCreateRequest paymentTerm;

}
