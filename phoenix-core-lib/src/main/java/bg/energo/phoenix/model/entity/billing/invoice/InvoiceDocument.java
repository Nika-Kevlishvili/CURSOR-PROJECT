package bg.energo.phoenix.model.entity.billing.invoice;

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
@Table(name = "invoice_documents", schema = "invoice")
public class InvoiceDocument extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "invoice_document_id_gen")
    @SequenceGenerator(name = "invoice_document_id_gen", schema = "invoice", sequenceName = "invoice_document_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "file_url")
    private String fileUrl;

    @NotNull
    @Column(name = "original_template_id", nullable = false)
    private Long originalTemplateId;

    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(name = "name")
    private String name;
}