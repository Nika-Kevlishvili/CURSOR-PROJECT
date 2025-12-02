package bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.ApplicationModelSubObjectStatus;
import bg.energo.phoenix.model.request.product.price.aplicationModel.ProfilesRequest;
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

@Table(name = "am_for_volumes_by_settlement_period_profiles", schema = "price_component")
public class SettlementPeriodsProfiles extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "am_for_volumes_by_settlement_period_profiles_id_seq",
            sequenceName = "price_component.am_for_volumes_by_settlement_period_profiles_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "am_for_volumes_by_settlement_period_profiles_id_seq"
    )
    private Long id;

    @Column(name = "percentage")
    private Double percentage;
    @ManyToOne
    @JoinColumn(name = "am_for_volumes_by_settlement_period_id")
    private VolumesBySettlementPeriod volumesBySettlementPeriod;


    @Column(name = "profile_id")
    private Long profileId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ApplicationModelSubObjectStatus status;

    public SettlementPeriodsProfiles(VolumesBySettlementPeriod model,ProfilesRequest request) {
        this.percentage = request.getPercentage();
        this.volumesBySettlementPeriod = model;
        this.profileId = request.getProfileId();
        this.status = ApplicationModelSubObjectStatus.ACTIVE;
    }

    public SettlementPeriodsProfiles(Double percentage, VolumesBySettlementPeriod volumesBySettlementPeriod, Long profileId) {
        this.percentage = percentage;
        this.volumesBySettlementPeriod = volumesBySettlementPeriod;
        this.profileId = profileId;
        this.status = ApplicationModelSubObjectStatus.ACTIVE;
    }
}
