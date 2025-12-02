package bg.energo.phoenix.model.entity.receivable.latePaymentFine;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.receivable.latePaymentFine.LatePaymentFineOutDocType;
import bg.energo.phoenix.model.enums.receivable.latePaymentFine.LatePaymentFineType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "late_payment_fines", schema = "receivable")
public class LatePaymentFine extends BaseEntity {
    @Id
    @SequenceGenerator(
            name = "late_payment_fines_id_seq",
            sequenceName = "receivable.late_payment_fines_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "late_payment_fines_id_seq")
    @Column(name = "id")
    private Long id;

    @Column(name = "late_payment_number")
    private String latePaymentNumber;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private LatePaymentFineType type;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "amount_in_other_ccy")
    private BigDecimal amountInOtherCcy;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "currency_id")
    private Long currencyId;

    @Column(name = "income_account_number")
    private String incomeAccountNumber;

    @Column(name = "cost_center_controlling_order")
    private String constCentreControllingOrder;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "contract_billing_group_id")
    private Long contractBillingGroupId;

    @Column(name = "issuer")
    private String issuer;

    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "document_template_id")
    private Long documentTemplateId;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "reversed")
    private boolean reversed;

    @Column(name = "reversal_late_payment_fine_id")
    private Long reversalLatePaymentId;

    @Column(name = "communication_id")
    private Long communicationId;

    @Column(name = "outgoing_doc_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private LatePaymentFineOutDocType outDocType;

    @Column(name = "logical_date")
    private LocalDate logicalDate;

    @Column(name = "parent_liability_id")
    private Long parentLiabilityId;

    @Column(name = "rescheduling_id")
    private Long reschedulingId;

    @Column(name = "parent_lpf_id")
    private Long parentLpfId;
}
