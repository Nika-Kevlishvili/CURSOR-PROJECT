package bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.nomenclature.product.priceComponent.Scales;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.VolumesByScaleScalesStatus;
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

@Table(name = "am_for_volumes_by_scale_scales", schema = "price_component")
public class VolumesByScaleScales extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "am_for_volumes_by_scale_scales_id_seq",
            sequenceName = "price_component.am_for_volumes_by_scale_scales_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "am_for_volumes_by_scale_scales_id_seq"
    )
    private Long id;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VolumesByScaleScalesStatus status;
    @ManyToOne
    @JoinColumn(name = "am_for_volumes_by_scale_id")
    private VolumesByScale volumesByScale;
    @ManyToOne
    @JoinColumn(name = "scale_id")
    private Scales scales;

    public VolumesByScaleScales(VolumesByScale volumesByScale, Scales scales) {
        this.status = VolumesByScaleScalesStatus.ACTIVE;
        this.volumesByScale = volumesByScale;
        this.scales = scales;
    }
}
