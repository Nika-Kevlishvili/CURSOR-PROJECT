package bg.energo.phoenix.model.entity.billing.billingRun;

import bg.energo.phoenix.model.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@Table(name = "billing_invoice_correction_invoices", schema = "billing")
public class BillingInvoiceCorrectionInvoice extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "billing_invoice_correction_invoices_id_gen")
    @SequenceGenerator(schema = "billing", name = "billing_invoice_correction_invoices_id_gen", sequenceName = "invoice_correction_invoices_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 50)
    @NotNull
    @Column(name = "invoice_number", nullable = false, length = 50)
    private String invoiceNumber;

    @NotNull
    @Column(name = "billing_id", nullable = false)
    private Long billingId;
}
