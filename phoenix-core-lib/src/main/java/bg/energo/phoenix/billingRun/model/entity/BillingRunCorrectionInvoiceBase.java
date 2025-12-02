package bg.energo.phoenix.billingRun.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;


@Entity
@Table(name = "correction_invoices_base", schema = "billing_run")
@Data
public class BillingRunCorrectionInvoiceBase {
    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "run_id")
    private Long runId;

    @Column(name = "old_run_id")
    private Long oldRunId;

    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(name = "contract_type")
    private String contractType;

    @Column(name = "product_contract_id")
    private Long productContractId;

    @Column(name = "service_contract_id")
    private Long serviceContractId;

    @Column(name = "is_active_record")
    private Boolean isActiveRecord;

    @Column(name = "reverse_status")
    private String reverseStatus;

    @Column(name = "error_message")
    private String errorMessage;
}
