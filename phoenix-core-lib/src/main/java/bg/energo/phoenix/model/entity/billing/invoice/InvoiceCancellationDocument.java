package bg.energo.phoenix.model.entity.billing.invoice;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@SuperBuilder
@Table(name = "invoice_cancellation_documents", schema = "invoice")
public class InvoiceCancellationDocument extends BaseEntity {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "invoice_cancellation_documents_id_seq")
    @SequenceGenerator(name = "invoice_cancellation_documents_id_seq", schema = "invoice", sequenceName = "invoice_cancellation_documents_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "cancelled_invoice_id")
    private Long cancelledInvoiceId;

    @Column(name = "document_id")
    private Long documentId;

    @Column(name = "status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

}
