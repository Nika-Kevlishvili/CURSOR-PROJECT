package bg.energo.phoenix.model.entity.pod.billingByProfile;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.pod.billingByProfile.BillingByProfileStatus;
import bg.energo.phoenix.model.enums.time.PeriodType;
import bg.energo.phoenix.model.enums.time.TimeZone;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor

@Table(name = "billing_by_profile", schema = "pod")
public class BillingByProfile extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "billing_by_profile_id_seq",
            sequenceName = "pod.billing_by_profile_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "billing_by_profile_id_seq"
    )
    private Long id;

    @Column(name = "pod_id")
    private Long podId;

    @Column(name = "period_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private PeriodType periodType;

    @Column(name = "time_zone")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private TimeZone timeZone;

    @Column(name = "profile_id")
    private Long profileId;

    @Column(name = "period_from")
    private LocalDateTime periodFrom;

    @Column(name = "period_to")
    private LocalDateTime periodTo;

    @Column(name = "invoiced")
    private Boolean invoiced;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private BillingByProfileStatus status;

}
