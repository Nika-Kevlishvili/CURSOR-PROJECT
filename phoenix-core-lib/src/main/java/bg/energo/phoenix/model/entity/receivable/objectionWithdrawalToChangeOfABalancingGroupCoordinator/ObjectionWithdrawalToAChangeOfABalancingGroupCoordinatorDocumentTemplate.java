package bg.energo.phoenix.model.entity.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.receivable.massOperationForBlocking.ReceivableSubObjectStatus;
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
@Table(name = "objection_withdrawal_to_change_of_cbg_doc_templates", schema = "receivable")

public class ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorDocumentTemplate extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "objection_withdrawal_to_change_of_cbg_doc_templates_id_seq",
            schema = "receivable",
            sequenceName = "objection_withdrawal_to_change_of_cbg_doc_templates_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "objection_withdrawal_to_change_of_cbg_doc_templates_id_seq"
    )
    private Long id;

    @Column(name = "withdrawal_change_of_cbg_id")
    private Long withdrawalChangeOfCbgId;

    @Column(name = "template_id")
    private Long templateId;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ReceivableSubObjectStatus status;
}
