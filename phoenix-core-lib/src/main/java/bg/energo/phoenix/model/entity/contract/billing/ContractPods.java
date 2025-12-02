package bg.energo.phoenix.model.entity.contract.billing;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "contract_pods", schema = "product_contract")
@EntityListeners(ContractPodEntityListener.class)
public class ContractPods extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "contract_pods_id_seq",
            sequenceName = "product_contract.contract_pods_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "contract_pods_id_seq"
    )
    private Long id;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

    @Column(name = "contract_detail_id")
    private Long contractDetailId;

    @Column(name = "activation_date")
    private LocalDate activationDate;

    @Column(name = "deactivation_date")
    private LocalDate deactivationDate;

    @Column(name = "deactivation_purpose_id")
    private Long deactivationPurposeId;

    @Column(name = "contract_billing_group_id")
    private Long billingGroupId;

    @Column(name = "split_id")
    private Long splitId;

    @Column(name = "custom_modify_date")
    private LocalDateTime customModifyDate;

    @Column(name = "pod_detail_id")
    private Long podDetailId;

    @Column(name = "deal_number")
    private String dealNumber;

    public ContractPods(Long billingGroupId, Long podDetailId, EntityStatus status, Long contractDetailId) {
        this.billingGroupId = billingGroupId;
        this.podDetailId = podDetailId;
        this.status = status;
        this.contractDetailId = contractDetailId;
    }

    public boolean isActiveInPerpetuity() {
        return activationDate != null && deactivationDate == null;
    }
}
