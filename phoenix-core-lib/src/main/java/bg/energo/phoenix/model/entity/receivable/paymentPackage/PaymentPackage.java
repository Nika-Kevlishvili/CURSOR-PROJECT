package bg.energo.phoenix.model.entity.receivable.paymentPackage;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.paymentPackage.PaymentPackageLockStatus;
import bg.energo.phoenix.model.enums.receivable.paymentPackage.PaymentPackageType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

@Entity
@Builder
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "payment_packages", schema = "receivable")
public class PaymentPackage extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "payment_packages_id_seq",
            schema = "receivable",
            sequenceName = "payment_packages_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "payment_packages_id_seq"
    )
    private Long id;

    @Column(name = "payment_package_status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private PaymentPackageLockStatus lockStatus;

    @Column(name = "payment_package_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private PaymentPackageType type;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status")
    private EntityStatus status;

    @Column(name = "collection_channel_id")
    private Long collectionChannelId;

    @Column(name = "account_period_id")
    private Long accountingPeriodId;

    @Column(name = "payment_date")
    private LocalDate paymentDate;
}
