package bg.energo.phoenix.model.entity.contract.action;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.contract.action.ActionPenaltyPayer;
import bg.energo.phoenix.model.enums.contract.action.ActionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "actions", schema = "action")
public class Action extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "actions_id_seq",
            sequenceName = "action.actions_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "actions_id_seq"
    )
    private Long id;

    @Column(name = "action_type_id")
    private Long actionTypeId;

    @Column(name = "notice_receiving_date")
    private LocalDate noticeReceivingDate;

    @Column(name = "execution_date")
    private LocalDate executionDate;

    @Column(name = "penalty_claim_amount")
    private BigDecimal penaltyClaimAmount;

    @Column(name = "penalty_claim_currency_id")
    private Long penaltyClaimCurrencyId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "penalty_payer")
    private ActionPenaltyPayer penaltyPayer;

    @Column(name = "dont_allow_auto_penalty_claim")
    private Boolean dontAllowAutomaticPenaltyClaim;

    @Column(name = "penalty_id")
    private Long penaltyId;

    @Column(name = "termination_id")
    private Long terminationId;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "product_contract_id")
    private Long productContractId;

    @Column(name = "service_contract_id")
    private Long serviceContractId;

    @Column(name = "additional_info")
    private String additionalInfo;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "action_status")
    private ActionStatus actionStatus;

    @Column(name = "calculated_penalty_amount")
    private BigDecimal calculatedPenaltyAmount;

    @Column(name = "calculated_penalty_currency_id")
    private Long calculatedPenaltyCurrencyId;

    @Column(name = "without_penalty")
    private Boolean withoutPenalty;

    @Column(name = "without_auto_termination")
    private Boolean withoutAutomaticTermination;

    @Column(name = "email_template_id")
    private Long emailTemplateId;

    @Column(name = "document_template_id")
    private Long templateId;

    @Column(name = "claim_amount_manually_entered")
    private Boolean claimAmountManuallyEntered;
}
