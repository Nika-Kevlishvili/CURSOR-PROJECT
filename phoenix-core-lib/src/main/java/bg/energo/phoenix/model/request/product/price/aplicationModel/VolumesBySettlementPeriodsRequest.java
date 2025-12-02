package bg.energo.phoenix.model.request.product.price.aplicationModel;

import bg.energo.phoenix.model.customAnotations.product.applicationModel.PeriodicityValidatorForSettlementPeriods;
import bg.energo.phoenix.model.customAnotations.product.applicationModel.RRuleValidator;
import bg.energo.phoenix.model.customAnotations.product.applicationModel.SettlementPeriodRequestValidator;
import bg.energo.phoenix.model.customAnotations.product.priceComponent.applicationModel.ProfilesRequestValidator;
import bg.energo.phoenix.model.customAnotations.product.priceComponent.applicationModel.ValueRangesValidator;
import bg.energo.phoenix.model.customAnotations.product.priceComponent.applicationModel.VolumeRangesValidator;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.Periodicity;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.PriceComponentTimeZone;
import bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.periodical.edit.EditDateOfMonthRequest;
import bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.periodical.edit.EditDayOfWeekPeriodOfYear;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Data
@PeriodicityValidatorForSettlementPeriods
public class VolumesBySettlementPeriodsRequest {

    @NotNull(message = "applicationModelRequest.settlementPeriodsRequest.periodType-Period Type is required;")
    private Periodicity periodType;

    @Valid
    private EditDayOfWeekPeriodOfYear dayOfWeekAndPeriodOfYear;

    private Set<@Valid EditDateOfMonthRequest> dateOfMonths;
    @RRuleValidator(message = "applicationModelRequest.settlementPeriodsRequest.formula")
    private String formula;

    private boolean hasVolumeRestriction;

    private boolean hasValueRestriction;

    private boolean yearRound;

    @VolumeRangesValidator(message = "applicationModelRequest.settlementPeriodsRequest.kwhRestriction")
    private List<VolumeRanges> kwhRestriction;

    @ValueRangesValidator(message = "applicationModelRequest.settlementPeriodsRequest.ccyRestriction")
    private List<ValueRanges> ccyRestriction;

    @DecimalMin(value = "0.01",message = "applicationModelRequest.settlementPeriodsRequest.kwhRestrictionPercent-Allowed range is between 0.01 and 100.00;")
    @DecimalMax(value = "100.00",message = "applicationModelRequest.settlementPeriodsRequest.kwhRestrictionPercent-Allowed range is between 0.01 and 100.00;")
    @Digits(integer = 3, fraction = 2, message = "applicationModelRequest.settlementPeriodsRequest.kwhRestrictionPercent-[kwhRestrictionPercent] should not exceed {integer} integral and {fraction} fractional parts;")
   // @NotNull(message = "applicationModelRequest.settlementPeriodsRequest.kwhRestrictionPercent-kwhRestrictionPercent can not be set to null;")
    private BigDecimal kwhRestrictionPercent;

    @ProfilesRequestValidator
    @NotEmpty(message = "applicationModelRequest.settlementPeriodsRequest.profiles-profiles can not be empty;")
    private List<ProfilesRequest> profiles;

    private PriceComponentTimeZone timeZone;

    @SettlementPeriodRequestValidator
    private List<SettlementPeriodRequest> settlementPeriods;

}
