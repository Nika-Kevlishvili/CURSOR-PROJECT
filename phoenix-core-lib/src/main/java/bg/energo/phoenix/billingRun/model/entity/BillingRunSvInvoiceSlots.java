package bg.energo.phoenix.billingRun.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "sv_invoice_slots", schema = "billing_run")
@Data
public class BillingRunSvInvoiceSlots {

    @Id
    @Column(name = "sv_invoice_slot_id")
    private Long svInvoiceSlotId;

    @Column(name = "run_id")
    private Long runId;

    @Column(name = "contract_id")
    private Long contractId;

    @Column(name = "pc_issued_separate_invoice")
    private String invoiceSlot;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "latest_contract_detail_id")
    private Long latestContractDetailId;

    @Column(name = "latest_product_detail_id")
    private Long latestProductDetailId;

    @Column(name = "latest_customer_detail_id")
    private Long latestCustomerDetailId;

    @Column(name = "customer_identifier")
    private String customerIdentifier;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "payment_term_id")
    private Long paymentTermId;

    @Column(name = "no_interest_on_overdue_debt")
    private Boolean noInterestOnOverdueDebt;

    @Column(name = "receipt_of_an_invoice_number")
    private Long receiptOfAnInvoiceNumber;

    @Column(name = "customer_communication_id")
    private Long customerCommunicationId;

    @Column(name = "number_of_income_account")
    private String numberOfIncomeAccount;

    @Column(name = "cost_center_controlling_order")
    private String costCenterControllingOrder;

    @Column(name = "direct_debit")
    private Boolean directDebit;

    @Column(name = "bank_id")
    private Long bankId;

    @Column(name = "bic")
    private String bic;

    @Column(name = "bank_account")
    private String bankAccount;

    @Column(name = "contract_communication_id")
    private Long contractCommunicationId;

    @Column(name = "interest_rate_id")
    private Long interestRateId;

    @Column(name = "status")
    private String status;

    @Column(name = "correction_run_id")
    private Long correctionRunId;
}
