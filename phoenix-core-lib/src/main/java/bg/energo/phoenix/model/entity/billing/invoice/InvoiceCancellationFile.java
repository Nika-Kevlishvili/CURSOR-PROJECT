package bg.energo.phoenix.model.entity.billing.invoice;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;


@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(schema = "invoice", name = "invoice_cancelation_files")
public class InvoiceCancellationFile extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "invoice_cancelation_files_id_seq", sequenceName = "invoice_cancelation_files_id_seq", schema = "invoice", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "invoice_cancelation_files_id_seq")
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "invoice_cancelation_id")
    private Long invoiceCancellationId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status")
    private EntityStatus status;

}
