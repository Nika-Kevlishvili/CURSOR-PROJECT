package bg.energo.phoenix.model.entity.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator.ObjectionWithdrawalToChangeOfCbgStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table(name = "objection_withdrawal_to_change_of_cbg", schema = "receivable")

public class ObjectionWithdrawalToAChangeOfABalancingGroupCoordinator extends BaseEntity {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "withdrawal_change_of_cbg_number")
    private String withdrawalChangeOfCbgNumber;

    @Column(name = "change_of_cbg_id")
    private Long changeOfCbgId;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "withdrawal_change_of_cbg_status")
    private ObjectionWithdrawalToChangeOfCbgStatus withdrawalToChangeOfCbgStatus;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private EntityStatus status;

    @Column(name = "email_template_id")
    private Long emailTemplateId;
}
