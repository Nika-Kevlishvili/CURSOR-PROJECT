package bg.energo.phoenix.model.entity.billing.invoice;

import bg.energo.phoenix.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(schema = "invoice", name = "deduction_interim_invoice")
public class DeductionInterimInvoice  extends BaseEntity {

    @Id
    @SequenceGenerator(schema = "invoice", allocationSize = 1, name = "deduction_interim_invoice_id_seq", sequenceName = "deduction_interim_invoice_id_seq")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "deduction_interim_invoice_id_seq")
    private Long id;

    @Column(name = "billing_run_id")
    private Long billingId;
    @Column(name = "invoice_id")
    private Long invoiceId;
    @Column(name = "interim_invoice_id")
    private Long interimInvoiceId;

    public DeductionInterimInvoice(Long billingId, Long invoiceId, Long interimInvoiceId) {
        this.billingId = billingId;
        this.invoiceId = invoiceId;
        this.interimInvoiceId = interimInvoiceId;
    }
}
