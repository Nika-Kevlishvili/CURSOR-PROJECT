package bg.energo.phoenix.model.entity.product.iap.interimAdvancePayment;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponent;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

@Entity
@Table(schema = "interim_advance_payment", name = "interim_advance_payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)

public class InterimAdvancePayment extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "interim_advance_payments_id_seq",
            sequenceName = "interim_advance_payment.interim_advance_payments_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "interim_advance_payments_id_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "value_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ValueType valueType;

    @Column(name = "value")
    private BigDecimal value;

    @Column(name = "value_from")
    private BigDecimal valueFrom;

    @Column(name = "value_to")
    private BigDecimal valueTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_id")
    private Currency currency;


    @Column(name = "date_of_issue_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private DateOfIssueType dateOfIssueType;

    @Column(name = "date_of_issue_value")
    private Integer dateOfIssueValue;

    @Column(name = "date_of_issue_value_from")
    private Integer dateOfIssueValueFrom;

    @Column(name = "date_of_issue_value_to")
    private Integer dateOfIssueValueTo;

    @ManyToOne
    @JoinColumn(name = "price_component_id")
    private PriceComponent priceComponent;

    @Column(name = "payment_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private PaymentType paymentType;

    @Column(name = "period_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private PeriodType periodType;

    @Column(name = "match_term_of_standard_invoice")
    private Boolean matchTermOfStandardInvoice;

    @Column(name = "no_interest_on_overdue_debts")
    private Boolean noInterestOnOverdueDebts;

    @Column(name = "year_round")
    private Boolean yearRound;

    @Column(name = "issuing_for_the_month_to_current")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private IssuingForTheMonthToCurrent issuingForTheMonthToCurrent;

    @Column(name = "deduction_from")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private DeductionFrom deductionFrom;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private InterimAdvancePaymentStatus status;

    @Column(name = "iap_group_detail_id")
    private Long groupDetailId;

}
