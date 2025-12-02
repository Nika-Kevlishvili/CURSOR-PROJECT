package bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.ApplicationModelSubObjectStatus;
import bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.periodical.PeriodOfYearBaseRequest;
import bg.energo.phoenix.model.request.product.price.aplicationModel.APPeriodOfYearRequest;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor

@Table(name = "am_for_volumes_by_scale_issuing_periods", schema = "price_component")
public class VolumesByScaleIssuingPeriods extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "am_for_volumes_by_scale_issuing_periods_id_seq",
            sequenceName = "price_component.am_for_volumes_by_scale_issuing_periods_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "am_for_volumes_by_scale_issuing_periods_id_seq"
    )
    private Long id;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ApplicationModelSubObjectStatus status;
    @Column(name = "period_from")
    private String periodFrom;
    @Column(name = "period_to")
    private String periodTo;
    @ManyToOne
    @JoinColumn(name = "am_for_volumes_by_scale_id")
    private VolumesByScale volumesByScale;

    public VolumesByScaleIssuingPeriods(PeriodOfYearBaseRequest request, VolumesByScale volumesByScale) {
        this.status = ApplicationModelSubObjectStatus.ACTIVE;
        this.periodFrom = request.getStartDate();
        this.periodTo = request.getEndDate();
        this.volumesByScale = volumesByScale;
    }

    public VolumesByScaleIssuingPeriods(APPeriodOfYearRequest request, VolumesByScale volumesByScale) {
        this.status = ApplicationModelSubObjectStatus.ACTIVE;
        this.periodFrom = request.getStartDate();
        this.periodTo = request.getEndDate();
        this.volumesByScale = volumesByScale;
    }
}
