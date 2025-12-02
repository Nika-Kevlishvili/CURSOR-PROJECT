package bg.energo.phoenix.model.entity.billing.invoice;

import bg.energo.phoenix.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "invoice_related_invoices", schema = "invoice")
public class InvoiceRelatedInvoice extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "invoice_related_invoice_id_gen")
    @SequenceGenerator(name = "invoice_related_invoice_id_gen", schema = "invoice", sequenceName = "invoice_related_invoice_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(name = "related_invoice_id")
    private Long relatedInvoiceId;
}