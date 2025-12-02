package bg.energo.phoenix.model.entity.billing.invoice;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceConnectionType;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceDocumentType;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStatus;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceType;
import bg.energo.phoenix.model.enums.contract.ContractType;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.DeductionFrom;
import jakarta.persistence.*;
import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Table(schema = "invoice", name = "invoices")
public class Invoice extends BaseEntity implements Cloneable {
    @Id
    @SequenceGenerator(schema = "invoice", allocationSize = 1, name = "invoices_id", sequenceName = "invoices_id_seq")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "invoices_id")
    private Long id;

    /**
     * Represents an invoice number.
     * <p>
     * An invoice number is a unique identifier for an invoice.
     * <p>
     * Format: Prefix-Number
     * <p>
     * Example for draft invoice: INV-D00000001
     * Example for draft invoice with generated PDF: INV-00000001
     */
    @Column(name = "invoice_number")
    @Setter(AccessLevel.NONE)
    private String invoiceNumber;
    @Column(name = "invoice_number_modify_date")
    @Setter(AccessLevel.NONE)
    private LocalDateTime invoiceNumberModifyDate;

    @Column(name = "invoice_date")
    private LocalDate invoiceDate;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status")
    private InvoiceStatus invoiceStatus;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "type")
    private InvoiceType invoiceType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "contract_type")
    private ContractType contractType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "document_type")
    private InvoiceDocumentType invoiceDocumentType;

    @Column(name = "tax_event_date")
    private LocalDate taxEventDate;

    @Column(name = "payment_deadline")
    private LocalDate paymentDeadline;

    @Column(name = "meter_reading_period_from")
    private LocalDate meterReadingPeriodFrom;

    @Column(name = "meter_reading_period_to")
    private LocalDate meterReadingPeriodTo;

    @Column(name = "basis_for_issuing")
    private String basisForIssuing;

    @Column(name = "income_account_number")
    private String incomeAccountNumber;

    @Column(name = "cost_center_controlling_order")
    private String costCenterControllingOrder;

    @Column(name = "applicable_interest_rate_id")
    private Long interestRateId;

    @Column(name = "direct_debit")
    private Boolean directDebit;

    @Column(name = "bank_id")
    private Long bankId;

    @Column(name = "iban")
    private String iban;

    @Column(name = "customer_detail_id")
    private Long customerDetailId;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "service_id")
    private Long serviceId;

    @Column(name = "pod_id")
    private Long podId;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "customer_communication_id")
    private Long customerCommunicationId;

    @Column(name = "contract_communication_id")
    private Long contractCommunicationId;

    @Column(name = "goods_order_id")
    private Long goodsOrderId;

    @Column(name = "service_order_id")
    private Long serviceOrderId;

    @Column(name = "contract_billing_group_id")
    private Long contractBillingGroupId;

    @Column(name = "product_contract_id")
    private Long productContractId;
    @Column(name = "product_contract_detail_id")
    private Long productContractDetailId;

    @Column(name = "service_contract_id")
    private Long serviceContractId;
    @Column(name = "service_contract_detail_id")
    private Long serviceContractDetailId;

    @Column(name = "billing_id")
    private Long billingId;

    @Column(name = "invoice_document_id")
    private Long invoiceDocumentId;

    @Column(name = "invoice_cancelation_id")
    private Long invoiceCancellationId;

    @Column(name = "invoice_cancellation_number")
    private String invoiceCancellationNumber;

    @Column(name = "currency_id")
    private Long currencyId;

    @Column(name = "currency_id_in_other_currency")
    private Long currencyIdInOtherCurrency;

    @Column(name = "currency_exchange_rate_on_invoice_creation")
    private BigDecimal currencyExchangeRateOnInvoiceCreation;

    @Column(name = "product_detail_id")
    private Long productDetailId;

    @Column(name = "service_detail_id")
    private Long serviceDetailId;

    @Column(name = "alternative_recipient_customer_detail_id")
    private Long alternativeRecipientCustomerDetailId;

    @Column(name = "account_period_id")
    private Long accountPeriodId;

    @Column(name = "total_amount_excluding_vat")
    private BigDecimal totalAmountExcludingVat;

    @Column(name = "total_amount_of_vat")
    private BigDecimal totalAmountOfVat;

    @Column(name = "total_amount_including_vat")
    private BigDecimal totalAmountIncludingVat;

    @Column(name = "total_amount_excluding_vat_in_other_currency")
    private BigDecimal totalAmountExcludingVatInOtherCurrency;

    @Column(name = "total_amount_of_vat_in_other_currency")
    private BigDecimal totalAmountOfVatInOtherCurrency;

    @Column(name = "total_amount_including_vat_in_other_currency")
    private BigDecimal totalAmountIncludingVatInOtherCurrency;

    @Column(name = "no_interest_on_overdue_debts")
    private Boolean noInterestOnOverdueDebts;

    @Column(name = "standard_invoice_id")
    private Long standardInvoiceId;

    @Column(name = "is_deducted")
    private Boolean isDeducted;

    @Column(name = "is_interim_match_invoice")
    private Boolean isInterimMatchInvoice;

    @Column(name = "interim_calculated_from_invoice_id")
    private Long interimCalculatedFromInvoiceId;

    @Column(name = "deducted_for_invoice_id")
    private Long deductedForInvoiceId;

    @Column(name = "issuing_for_the_month")
    private LocalDate issuingForTheMonth;

    @Column(name = "issuing_for_payment_term_date")
    private LocalDate issuingForPaymentTermDate;

    @Column(name = "invoice_slot")
    private String invoiceSlot;

    @Column(name = "processor_row_id")
    private Long processorRowId;

    //@Column(name = "generate_equal_monthly")
    //private Boolean generateEqualMonthly;

    @Column(name = "deduction_from_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private DeductionFrom deductionFromType;

    @Column(name = "reversal_created_from_id")
    private Long reversalCreatedFromId;

    @Column(name = "template_detail_id")
    private Long templateDetailId;

    @Column(name = "compensation_index")
    private Integer compensationIndex;

    @Column(name = "total_actual_consumption")
    private BigDecimal totalActualConsumption;

    @Column(name = "total_actual_consumption_amount")
    private BigDecimal totalActualConsumptionAmount;

    @Column(name = "has_one_time_on_time_invoice_detail")
    private Boolean hasOneTimeOnTimeInvoiceDetail;

    @Column(name = "has_with_electricity_invoice_detail")
    private Boolean hasWithElectricityInvoiceDetail;

    @Column(name = "price_was_changed")
    private Boolean priceWasChanged;

    @Column(name = "current_status_change_date")
    private LocalDateTime currentStatusChangeDate;

    @Column(name = "parent_invoice_id")
    private Long parentInvoiceId;

    public void setInvoiceNumber(String invoiceNumber) {
        if (!StringUtils.equals(invoiceNumber, this.invoiceNumber)) {
            this.invoiceNumberModifyDate = LocalDateTime.now();
        }
        this.invoiceNumber = invoiceNumber;
    }

    @Override
    public Invoice clone() {
        try {
            return (Invoice) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    public List<Invoice> clone(int nOfCopies) {
        List<Invoice> invoices = new ArrayList<>();
        for (int i = 0; i < nOfCopies; i++) {
            invoices.add(this.clone());
        }
        return invoices;
    }

    public InvoiceConnectionType type() {
        if (Objects.nonNull(this.productContractId)) {
            return InvoiceConnectionType.PRODUCT_CONTRACT;
        } else if (Objects.nonNull(this.serviceContractId)) {
            return InvoiceConnectionType.SERVICE_CONTRACT;
        } else if (Objects.nonNull(this.goodsOrderId)) {
            return InvoiceConnectionType.GOODS_ORDER;
        } else if (Objects.nonNull(this.serviceOrderId)) {
            return InvoiceConnectionType.SERVICE_ORDER;
        } else if (Objects.nonNull(this.customerDetailId)) {
            return InvoiceConnectionType.CUSTOMER;
        } else {
            throw new RuntimeException("Cannot define invoice connection type");
        }
    }

    public void setInvoiceStatus(InvoiceStatus status) {
        if (!Objects.equals(status, InvoiceStatus.DRAFT_GENERATED)) {
            if (!Objects.equals(this.invoiceStatus, status)) {
                currentStatusChangeDate = LocalDateTime.now();
            }
        }
        this.invoiceStatus = status;
    }

    public static class InvoiceBuilder {
        public InvoiceBuilder invoiceStatus(InvoiceStatus status) {
            if (!Objects.equals(status, InvoiceStatus.DRAFT_GENERATED)) {
                if (!Objects.equals(this.invoiceStatus, status)) {
                    currentStatusChangeDate = LocalDateTime.now();
                }
            }
            this.invoiceStatus = status;
            return this;
        }
    }

//If you add new fields also add it in InvoiceReversalProcessService. invoiceReversal() .
}
