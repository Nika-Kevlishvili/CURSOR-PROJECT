package bg.energo.phoenix.model.entity.billing.invoice;

import bg.energo.phoenix.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "invoice_document_files", schema = "invoice")
public class InvoiceDocumentFile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "invoice_document_files_id_seq")
    @SequenceGenerator(name = "invoice_document_files_id_seq", schema = "invoice", sequenceName = "invoice_document_files_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "document_id")
    private Long documentId;

    @Column(name = "invoice_id")
    private Long invoiceId;

/*
    @Column(name = "email_sent")
    private Boolean emailSent;
*/

}
