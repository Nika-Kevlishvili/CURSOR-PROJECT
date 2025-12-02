package bg.energo.phoenix.model.entity.receivable.paymentPackage;

import bg.energo.phoenix.model.enums.receivable.paymentPackage.PaymentPackageLockStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedBy;

import java.time.LocalDateTime;

@Entity
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "payment_package_status_change_hist", schema = "receivable")
public class PaymentPackageStatusChangeHistory {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "payment_package_status_change_hist_id_seq",
            schema = "receivable",
            sequenceName = "payment_package_status_change_hist_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "payment_package_status_change_hist_id_seq"
    )
    private Long id;

    @Column(name = "payment_package_id")
    private Long paymentPackageId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private PaymentPackageLockStatus lockStatus;

    @Column(name = "system_user_id", nullable = false, length = 50)
    @CreatedBy
    private String systemUserId;

    @CreationTimestamp
    @Column(name = "create_date", nullable = false)
    private LocalDateTime createDate;
}
