package bg.energo.phoenix.model.entity.billing.compensation;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.billing.compensation.CompensationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "compensations", schema = "billing")
public class Compensations extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "compensations_id_seq",
            sequenceName = "billing.compensations_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "compensations_id_seq"
    )
    private Long id;

    @Column(name = "compensation_document_number")
    private String compensationDocumentNumber;

    @Column(name = "compensation_document_date")
    private LocalDate compensationDocumentDate;

    @Column(name = "compensation_document_volumes")
    private BigDecimal compensationDocumentVolumes;

    @Column(name = "compensation_document_price")
    private BigDecimal compensationDocumentPrice;

    @Column(name = "compensation_reason")
    private String compensationReason;

    @Column(name = "compensation_document_period")
    private LocalDate compensationDocumentPeriod;

    @Column(name = "compensation_document_amount")
    private BigDecimal compensationDocumentAmount;

    @Column(name = "compensation_document_currency_id")
    private Long compensationDocumentCurrencyId;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "pod_id")
    private Long podId;

    @Column(name = "recipient_id")
    private Long recipientId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "compensation_status")
    private CompensationStatus compensationStatus;

    @Column(name = "compensation_index")
    private Integer compensationIndex;

    @Column(name = "invoice_usage_date")
    private LocalDate invoiceUsageDate;

    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(name = "liability_for_recipient_id")
    private Long liabilityForRecipientId;

    @Column(name = "receivable_for_customer_id")
    private Long receivableForCustomerId;

    @Column(name = "liability_for_customer_id")
    private Long liabilityForCustomerId;

    @Column(name = "receivable_for_recipient_id")
    private Long receivableForRecipientId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status")
    private EntityStatus status;

}
