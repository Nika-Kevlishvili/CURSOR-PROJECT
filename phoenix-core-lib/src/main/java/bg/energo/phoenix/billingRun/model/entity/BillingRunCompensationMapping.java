package bg.energo.phoenix.billingRun.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "compensation_mapping", schema = "billing_run")
@Data
public class BillingRunCompensationMapping {
    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "bg_invoice_slot_id")
    private Long bgInvoiceSlotId;

    @Column(name = "compensation_id")
    private Long compensationId;

    @Column(name = "run_id")
    private Long runId;
}
