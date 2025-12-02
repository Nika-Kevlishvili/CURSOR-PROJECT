package bg.energo.phoenix.model.entity.product.penalty.penalty;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import io.hypersistence.utils.hibernate.type.array.internal.AbstractArrayType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "penalties", schema = "terms")
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@Setter
@NoArgsConstructor
@Builder
public class Penalty extends BaseEntity {
    @Id
    @SequenceGenerator(name = "penalties_id_seq", sequenceName = "terms.penalties_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "penalties_id_seq")
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "contract_clause_number")
    private String contractClauseNumber;

    @Column(name = "process_id")
    private Long processId; // TODO: implement later

    @Column(name = "process_start_code")
    private String processStartCode; // TODO: implement later

    @Enumerated(EnumType.STRING)
    @Column(name = "applicability")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private PenaltyApplicability applicability;

    @Column(name = "amount_calculation_formula")
    private String amountCalculationFormula;

    @Column(name = "min_amount")
    private BigDecimal minAmount;

    @Column(name = "max_amount")
    private BigDecimal maxAmount;

    @Column(name = "currency_id")
    private Long currencyId;

    @Column(name = "automatic_submission")
    private boolean automaticSubmission;

    @Column(name = "additional_info")
    private String additionalInfo;

    @Column(name = "term_id")
    private Long termId;


    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "terms.penalty_party_receiver"
            )
    )
    @Column(name = "penalty_party_receiver", columnDefinition = "terms.penalty_party_receiver[]")
    private List<PartyReceivingPenalty> partyReceivingPenalties;

    private Long penaltyGroupDetailId;

    @Column(name = "no_interest_on_overdue_debts")
    private Boolean noInterestOnOverdueDebts;

    @Column(name = "email_template_id")
    private Long emailTemplateId;

    @Column(name = "template_id")
    private Long templateId;

    public Penalty(Long id, String name, String contractClauseNumber, Long processId, String processStartCode, PenaltyApplicability applicability, String amountCalculationFormula, BigDecimal minAmount, BigDecimal maxAmount, Long currencyId, boolean automaticSubmission, String additionalInfo, Long termId, EntityStatus status, List<PartyReceivingPenalty> partyReceivingPenalties, Long penaltyGroupDetailId, Boolean noInterestOnOverdueDebts) {
        this.id = id;
        this.name = name;
        this.contractClauseNumber = contractClauseNumber;
        this.processId = processId;
        this.processStartCode = processStartCode;
        this.applicability = applicability;
        this.amountCalculationFormula = amountCalculationFormula;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.currencyId = currencyId;
        this.automaticSubmission = automaticSubmission;
        this.additionalInfo = additionalInfo;
        this.termId = termId;
        this.status = status;
        this.partyReceivingPenalties = partyReceivingPenalties;
        this.penaltyGroupDetailId = penaltyGroupDetailId;
        this.noInterestOnOverdueDebts = noInterestOnOverdueDebts;
    }
}
