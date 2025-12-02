package bg.energo.phoenix.model.response.priceComponent.applicationModel;

import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.Periodicity;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.PriceComponentTimeZone;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class VolumesBySettlementPeriodResponse {
    private Periodicity periodType;
    private String formula;
    private Boolean hasVolumeRestriction;
    private Boolean hasValueRestriction;
    private Boolean yearRound;
    private BigDecimal volumeRestrictionPercent;
    private PriceComponentTimeZone timeZone;

    private List<ProfileResponse> profileResponses;
    private List<SettlementPeriodsResponse> settlementPeriods;


    private List<KwhRestrictionResponse> kwhRestriction;
    private List<CcyRestrictionResponse> ccyRestriction;

    private List<ApplicationModelDateOfMonthResponse> dateOfMonths;
    private ApplicationModelDayWeekPeriodOfYearResponse dayWeekPeriodOfYear;

}
