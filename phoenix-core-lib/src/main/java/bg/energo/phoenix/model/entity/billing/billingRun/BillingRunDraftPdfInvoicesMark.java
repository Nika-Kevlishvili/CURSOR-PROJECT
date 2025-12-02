package bg.energo.phoenix.model.entity.billing.billingRun;

import bg.energo.phoenix.model.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "billing_run_draft_pdf_invoices_marks", schema = "billing")
public class BillingRunDraftPdfInvoicesMark extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "billing_run_draft_pdf_invoices_marks_id_gen")
    @SequenceGenerator(name = "billing_run_draft_pdf_invoices_marks_id_gen", sequenceName = "billing_run_draft_generated_invoices_marks_id_seq", schema = "billing", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @Column(name = "billing_run_id", nullable = false)
    private Long billingRun;

    @NotNull
    @Column(name = "invoice_id", nullable = false)
    private Long invoice;
}