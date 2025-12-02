package bg.energo.phoenix.model.entity.billing.billingRun;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.billing.billings.*;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.DeductionFrom;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.IssuingForTheMonthToCurrent;
import bg.energo.phoenix.model.enums.product.price.priceComponent.IssuedSeparateInvoice;
import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import io.hypersistence.utils.hibernate.type.array.internal.AbstractArrayType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(name = "billings", schema = "billing")
public class BillingRun extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "billings_id_seq",
            sequenceName = "billing.billings_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "billings_id_seq"
    )
    private Long id;
    @Column(name = "billing_number")
    private String billingNumber;

    @Column(name = "run_periodicity")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private BillingRunPeriodicity runPeriodicity;

    @Column(name = "additional_info")
    private String additionalInfo;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private BillingType type;

    @Column(name = "billing_run_process_stage")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private BillingRunProcessStage processStage;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private BillingStatus status;

    @Column(name = "tax_event_date")
    private LocalDate taxEventDate;

    @Column(name = "invoice_date")
    private LocalDate invoiceDate;

    @Column(name = "account_period_id")
    private Long accountingPeriodId;

    @Column(name = "invoice_due_date_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private InvoiceDueDateType invoiceDueDateType;

    @Column(name = "invoice_due_date")
    private LocalDate invoiceDueDate;

    @Column(name = "application_model_type", columnDefinition = "billing.billing_application_model_type[]")
    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "billing.billing_application_model_type"
            )
    )
    private List<ApplicationModelType> applicationModelType;

    @Column(name = "sending_an_invoice")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private SendingAnInvoice sendingAnInvoice;

    @Column(name = "execution_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ExecutionType executionType;

    @Column(name = "execution_date")
    private LocalDateTime executionDate;

    @Column(name = "criteria")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private BillingCriteria billingCriteria;

    @Column(name = "application_level")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private BillingApplicationLevel applicationLevel;

    @Column(name = "customer_contract_or_pod_conditions")
    private String customerContractOrPodConditions;

    @Column(name = "customer_contract_or_pod_list")
    private String customerContractOrPodList;

    @Column(name = "run_stage", columnDefinition = "billing.billing_run_stage[]")
    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "billing.billing_run_stage"
            )
    )
    private List<RunStage> runStages;

    // interim and advance payment field

    @Column(name = "vat_rate_id")
    private Long vatRateId;

    @Column(name = "global_vat_rate")
    private Boolean globalVatRate;

    @Column(name = "applicable_interest_rate_id")
    private Long interestRateId;
    @Column(name = "bank_id")
    private Long bankId;

    @Column(name = "iban")
    private String iban;

    @Column(name = "amount_excluding_vat")
    private BigDecimal amountExcludingVat;

    @Column(name = "issuing_for_month_to_current")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private IssuingForTheMonthToCurrent issuingForTheMonthToCurrent;

    @Column(name = "invoice_type", columnDefinition = "billing.billing_invoice_type[]")
    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "billing.billing_invoice_type"
            )
    )
    private List<IssuedSeparateInvoice> issuedSeparateInvoices;

    @Column(name = "amount_excluding_vat_currency_id")
    private Long currencyId;

    @Column(name = "deduction_from")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private DeductionFrom deductionFrom;

    @Column(name = "income_account_number")
    private String numberOfIncomeAccount;

    @Column(name = "cost_center_controlling_order")
    private String costCenterControllingOrder;

    @Column(name = "customer_detail_id")
    private Long customerDetailId;

    @Column(name = "product_contract_id")
    private Long productContractId;

    @Column(name = "service_contract_id")
    private Long serviceContractId;

    @Column(name = "document_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private DocumentType documentType;

    @Column(name = "goods_order_id")
    private Long goodsOrderId;

    @Column(name = "service_order_id")
    private Long serviceOrderId;

    @Column(name = "basic_for_issuing")
    private String basisForIssuing;

    @Column(name = "manual_invoice_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ManualInvoiceType manualInvoiceType;

    @Column(name = "direct_debit")
    private Boolean directDebit;

    @Column(name = "customer_communication_id")
    private Long customerCommunicationId;

    @Column(name = "invoice_list")
    private String listOfInvoices;

    @Column(name = "price_change")
    private Boolean priceChange;
    @Column(name = "volume_change")
    private Boolean volumeChange;

    @Column(name = "periodicity_created_from_id")
    private Long periodicityCreatedFromId;

    @Column(name = "employee_id")
    private Long employeeId;
    @Column(name = "periodic_billing_created_from")
    private Long periodicBillingCreatedFrom;

    @Column(name = "prefix_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private PrefixType prefixType;

    @Column(name = "periodic_max_end_date")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private BillingEndDate periodicMaxEndDate;

    @Column(name = "periodic_max_end_date_value")
    private Integer periodicMaxEndDateValue;

    @Column(name = "max_end_date")
    private LocalDate maxEndDate;

    @Column(name = "template_id")
    private Long templateId;
    @Column(name = "email_template_id")
    private Long emailTemplateId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "run_main_data_preparation_status")
    private BillingRunDataPreparationStatus mainDataPreparationStatus;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "run_interim_data_preparation_status")
    private BillingRunDataPreparationStatus interimDataPreparationStatus;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "run_interim_invoice_generation_status")
    private BillingRunDataPreparationStatus interimInvoiceGenerationStatus;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "run_main_inovice_generation_status")
    private BillingRunMainInvoiceGenerationStatus mainInvoiceGenerationStatus;

}
