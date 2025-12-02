package bg.energo.phoenix.model.response.priceParameter;

import bg.energo.phoenix.model.entity.product.price.priceParameter.PriceParameterDetailInfo;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PriceParameterDetailInfoPreviewResponse {
    private Long id;
    private BigDecimal price;
    private Boolean isShiftedHour;
    private LocalDateTime periodFrom;
    private LocalDateTime periodTo;
    private Integer periodFromYearsUnit;
    private Integer periodFromMonthsUnit;
    private Integer periodFromDaysUnit;
    private Integer periodFromHoursUnit;
    private Integer periodFromMinutesUnit;
    private Integer periodToYearsUnit;
    private Integer periodToMonthsUnit;
    private Integer periodToDaysUnit;
    private Integer periodToHoursUnit;
    private Integer periodToMinutesUnit;

    public PriceParameterDetailInfoPreviewResponse(PriceParameterDetailInfo priceParameterDetailInfo) {
        this.id = priceParameterDetailInfo.getId();
        this.price = priceParameterDetailInfo.getPrice();
        this.isShiftedHour = priceParameterDetailInfo.getIsShiftedHour();
        this.periodFrom = priceParameterDetailInfo.getPeriodFrom();
        this.periodTo = priceParameterDetailInfo.getPeriodTo();
        this.periodFromYearsUnit = priceParameterDetailInfo.getPeriodFrom().getYear();
        this.periodFromMonthsUnit = priceParameterDetailInfo.getPeriodFrom().getMonth().getValue();
        this.periodFromDaysUnit = priceParameterDetailInfo.getPeriodFrom().getDayOfMonth();
        this.periodFromHoursUnit = priceParameterDetailInfo.getPeriodFrom().getHour();
        this.periodFromMinutesUnit = priceParameterDetailInfo.getPeriodFrom().getMinute();
        this.periodToYearsUnit = priceParameterDetailInfo.getPeriodTo().getYear();
        this.periodToMonthsUnit = priceParameterDetailInfo.getPeriodTo().getMonth().getValue();
        this.periodToDaysUnit = priceParameterDetailInfo.getPeriodTo().getDayOfMonth();
        this.periodToHoursUnit = priceParameterDetailInfo.getPeriodTo().getHour();
        this.periodToMinutesUnit = priceParameterDetailInfo.getPeriodTo().getMinute();
    }
}
