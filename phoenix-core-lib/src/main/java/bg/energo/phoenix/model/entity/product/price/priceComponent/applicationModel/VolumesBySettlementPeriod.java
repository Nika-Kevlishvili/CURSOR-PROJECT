package bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.ApplicationModelSubObjectStatus;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.Periodicity;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.PriceComponentTimeZone;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

@Entity
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor

@Table(name = "am_for_volumes_by_settlement_periods", schema = "price_component")
public class VolumesBySettlementPeriod extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "am_for_volumes_by_settlement_periods_id_seq",
            sequenceName = "price_component.am_for_volumes_by_settlement_periods_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "am_for_volumes_by_settlement_periods_id_seq"
    )
    private Long id;

    @Column(name = "periodicity")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private Periodicity periodicity;

    @Column(name = "time_zone")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private PriceComponentTimeZone timeZone;

    @Column(name = "rrule_formula")
    private String rruleFormula;

    @Column(name = "year_round")
    private Boolean yearRound;

    @Column(name = "restriction_of_application_based_on_values")
    private Boolean isRestrictedBasedOnValue;

    @Column(name = "restriction_of_application_based_on_volume")
    private Boolean isRestrictedBasedOnVolume;

    @Column(name = "restriction_of_application_based_on_volume_percent")
    private BigDecimal volumeRestrictionPercent;

    @ManyToOne
    @JoinColumn(name = "application_model_id")
    private ApplicationModel applicationModel;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ApplicationModelSubObjectStatus status;

}
