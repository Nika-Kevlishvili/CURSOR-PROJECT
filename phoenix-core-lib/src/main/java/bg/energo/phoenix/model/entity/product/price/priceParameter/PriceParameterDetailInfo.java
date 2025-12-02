package bg.energo.phoenix.model.entity.product.price.priceParameter;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.time.PeriodType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor

@Table(name = "price_parameter_detail_info", schema = "prices")
public class PriceParameterDetailInfo extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "price_parameter_detail_info_id_seq",
            sequenceName = "prices.price_parameter_detail_info_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "price_parameter_detail_info_id_seq"
    )
    private Long id;

    @Column(name = "period_from")
    private LocalDateTime periodFrom;

    @Column(name = "period_to")
    private LocalDateTime periodTo;

    @Column(name = "price")
    private BigDecimal price;

    @Builder.Default
    @Column(name = "is_shifted_hour")
    private Boolean isShiftedHour = false;

    @Column(name = "price_parameter_detail_id")
    private Long priceParameterDetailId;

    public PriceParameterDetailInfo(LocalDateTime  periodFrom, Long priceDetailId,boolean isShiftedHour, PeriodType type) {
        this.periodFrom = periodFrom;
        this.priceParameterDetailId=priceDetailId;
        switch (type){
            case FIFTEEN_MINUTES -> this.periodTo = periodFrom.plus(15, ChronoUnit.MINUTES);
            case ONE_HOUR -> this.periodTo = periodFrom.plus(1, ChronoUnit.HOURS);
            case ONE_DAY -> this.periodTo = periodFrom.plus(1, ChronoUnit.DAYS);
            case ONE_MONTH -> this.periodTo = periodFrom.plus(periodFrom.getMonth().maxLength(), ChronoUnit.DAYS);
        }
        this.isShiftedHour=isShiftedHour;
    }

    public PriceParameterDetailInfo deepClone(PriceParameterDetailInfo info,Long priceDetailId){
        PriceParameterDetailInfo newInfo = new PriceParameterDetailInfo();
        newInfo.setPrice(info.getPrice());
        newInfo.setPeriodFrom(info.getPeriodFrom());
        newInfo.setPriceParameterDetailId(priceDetailId);
        newInfo.setPeriodTo(info.getPeriodTo());
        newInfo.setIsShiftedHour(info.getIsShiftedHour());
        return newInfo;
    }
    public PriceParameterDetailInfo(LocalDateTime periodFrom, LocalDateTime periodTo, BigDecimal price, Boolean isShiftedHour, Long priceParameterDetailId) {
        this.periodFrom = periodFrom;
        this.periodTo = periodTo;
        this.price = price;
        this.isShiftedHour = isShiftedHour;
        this.priceParameterDetailId = priceParameterDetailId;
    }
}
