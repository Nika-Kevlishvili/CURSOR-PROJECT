package bg.energo.phoenix.model.entity.product.iap.advancedPaymentGroup;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.product.iap.advancedPaymentGroup.AdvancedPaymentGroupStatus;
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

@Table(name = "iap_group_iaps", schema = "interim_advance_payment")
public class AdvancedPaymentGroupAdvancedPayments extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "iap_group_iaps_id_seq",
            sequenceName = "interim_advance_payment.iap_group_iaps_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "iap_group_iaps_id_seq"
    )
    private Long id;

    @Column(name = "interim_advance_payment_id")
    private Long advancePaymentId;

    @Column(name = "interim_advance_payment_group_detail_id")
    private Long advancePaymentGroupDetailId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private AdvancedPaymentGroupStatus status;
}
