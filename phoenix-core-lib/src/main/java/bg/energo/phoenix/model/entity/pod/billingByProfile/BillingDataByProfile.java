package bg.energo.phoenix.model.entity.pod.billingByProfile;

import bg.energo.phoenix.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor

@Table(name = "billing_data_by_profile", schema = "pod")
public class BillingDataByProfile extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "billing_data_by_profile_id_seq",
            sequenceName = "pod.billing_data_by_profile_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "billing_data_by_profile_id_seq"
    )
    private Long id;

    @Column(name = "period_from")
    private LocalDateTime periodFrom;

    @Column(name = "period_to")
    private LocalDateTime periodTo;

    @Column(name = "value")
    private BigDecimal value;

    @Builder.Default
    @Column(name = "is_shifted_hour")
    private Boolean isShiftedHour = false;

    @Column(name = "billing_by_profile_id")
    private Long billingByProfileId;

}
