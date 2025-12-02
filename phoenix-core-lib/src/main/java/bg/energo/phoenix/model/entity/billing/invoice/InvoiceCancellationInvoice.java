package bg.energo.phoenix.model.entity.billing.invoice;

import bg.energo.phoenix.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "invoice_cancellation_invoices", schema = "invoice")
public class InvoiceCancellationInvoice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "invoice_cancellation_invoices_id_seq")
    @SequenceGenerator(name = "invoice_cancellation_invoices_id_seq", schema = "invoice", sequenceName = "invoice_cancellation_invoices_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(name = "invoice_cancellation_id")
    private Long invoiceCancellationId;
}
