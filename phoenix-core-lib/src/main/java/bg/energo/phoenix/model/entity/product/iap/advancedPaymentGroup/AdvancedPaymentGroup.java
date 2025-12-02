package bg.energo.phoenix.model.entity.product.iap.advancedPaymentGroup;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.product.iap.advancedPaymentGroup.AdvancedPaymentGroupStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor

@Table(name = "interim_advance_payment_groups", schema = "interim_advance_payment")
public class AdvancedPaymentGroup extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "interim_advance_payment_groups_id_seq",
            sequenceName = "interim_advance_payment.interim_advance_payment_groups_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "interim_advance_payment_groups_id_seq"
    )
    private Long id;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private AdvancedPaymentGroupStatus status;

    public AdvancedPaymentGroup(AdvancedPaymentGroupStatus status) {
        this.status = status;
    }
}
