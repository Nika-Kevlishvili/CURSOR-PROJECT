package bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.ApplicationModelSubObjectStatus;
import bg.energo.phoenix.model.request.product.price.aplicationModel.VolumeRanges;
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

@Table(name = "am_for_volumes_by_scale_kwh_restriction_ranges", schema = "price_component")
public class VolumesByScaleKwhRestriction extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "am_for_volumes_by_scale_kwh_restriction_ranges_id_seq",
            sequenceName = "price_component.am_for_volume_by_scale_kwh_restriction_ranges_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "am_for_volumes_by_scale_kwh_restriction_ranges_id_seq"
    )
    private Long id;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ApplicationModelSubObjectStatus status;

    @Column(name = "value_from")
    private Integer valueFrom;
    @Column(name = "value_to")
    private Integer valueTo;
    @ManyToOne
    @JoinColumn(name = "am_for_volumes_by_scale_id")
    private VolumesByScale volumesByScale;

    public VolumesByScaleKwhRestriction(VolumeRanges ranges, VolumesByScale volumesByScale) {
        this.status = ApplicationModelSubObjectStatus.ACTIVE;
        this.valueFrom = ranges.getValueFrom();
        this.valueTo = ranges.getValueTo();
        this.volumesByScale = volumesByScale;
    }


}
