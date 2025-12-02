package bg.energo.phoenix.billingRun.model.entity;

import bg.energo.phoenix.model.enums.contract.ContractType;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.DeductionFrom;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.IssuingForTheMonthToCurrent;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "run_interim_data", schema = "billing_run")
public class BillingRunInterimData {
    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "is_valid_for_generation")
    private Boolean isValidForGeneration;

    @Column(name = "run_id", nullable = false)
    private Long runId;

    @Column(name = "contract_id")
    private Long contractId;

    @Column(name = "contract_detail_id")
    private Long contractDetailId;

    @Column(name = "product_detail_id")
    private Long productDetailId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "contract_type")
    private ContractType contractType;

    @Column(name = "billing_group_id")
    private Long billingGroupId;

    @Column(name = "pod_id")
    private Long podId;

    @Column(name = "interim_id")
    private Long interimId;

    @Column(name = "value_type")
    private String valueType;

    @Column(name = "calculation_value")
    private BigDecimal calculationValue;

    @Column(name = "currency_id")
    private Long currencyId;

    @Column(name = "date_of_issue_type")
    private String dateOfIssueType;

    @Column(name = "payment_term_value")
    private Integer paymentTermValue;

    @Column(name = "price_component_id")
    private Long priceComponentId;

    @Column(name = "payment_term_id")
    private Long paymentTermId;

    @Column(name = "price_formula")
    private String priceFormula;

    @Column(name = "issued_separate_invoice")
    private String issuedSeparateInvoice;

    @Column(name = "vat_rate_id")
    private Long vatRateId;

    @Column(name = "global_vat_rate")
    private Boolean globalVatRate;

    @Column(name = "prev_invoice_id")
    private Long prevInvoiceId;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "issuing_for_the_month_to_current")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private IssuingForTheMonthToCurrent issuingForTheMonthToCurrent;

    @Column(name = "deduction_from")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private DeductionFrom deductionFrom;

    @Column(name = "match_term_of_standard_invoice")
    private Boolean matchTermOfStandardInvoice;

    @Column(name = "status")
    private String status;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "customer_detail_id")
    private Long customerDetailId;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "cost_center_controlling_order")
    private String costCenterControllingOrder;

    @Column(name = "number_of_income_account")
    private String numberOfIncomeAccount;

    @Column(name = "applicable_interest_rate")
    private Long interestRateId;

    @Column(name = "receipt_of_an_invoice_number")
    private Long receiptOfAnInvoiceNumber;

    @Column(name = "customer_communication_id")
    private Long customerCommunicationId;

    @Column(name = "contract_communication_id")
    private Long contractCommunicationId;

    @Column(name = "bank_id")
    private Long bankId;

    @Column(name = "bank_account")
    private String iban;

    @Column(name = "direct_debit")
    private Boolean directDebit;

    @Column(name = "no_interest_on_overdue_debt")
    private Boolean noInterestOnOverdueDebt;
}
