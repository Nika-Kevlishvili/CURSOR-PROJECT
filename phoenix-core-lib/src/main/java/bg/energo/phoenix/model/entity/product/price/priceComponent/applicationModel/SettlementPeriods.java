package bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.ApplicationModelSubObjectStatus;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.MinuteRange;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.SettlementPeriodHours;
import bg.energo.phoenix.model.request.product.price.aplicationModel.SettlementPeriodRequest;
import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import io.hypersistence.utils.hibernate.type.array.internal.AbstractArrayType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "am_settlement_periods", schema = "price_component")
public class SettlementPeriods extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "am_settlement_periods_id_seq",
            sequenceName = "price_component.am_settlement_periods_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "am_settlement_periods_id_seq"
    )
    private Long id;

    @Column(name = "minute_range")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private MinuteRange minuteRange;

    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "price_component.am_settlement_period_hours"
            )
    )
    @Column(name = "hours", columnDefinition = "price_component.am_settlement_period_hours[]")
    private List<SettlementPeriodHours> hours;


    @ManyToOne
    @JoinColumn(name = "am_for_volumes_by_settlement_period_id")
    private VolumesBySettlementPeriod volumesBySettlementPeriod;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ApplicationModelSubObjectStatus status;

    public SettlementPeriods(VolumesBySettlementPeriod model, SettlementPeriodRequest request) {
        this.minuteRange = request.getMinuteRange();
        this.hours = request.getHours();
        this.volumesBySettlementPeriod = model;
        this.status = ApplicationModelSubObjectStatus.ACTIVE;
    }
}
