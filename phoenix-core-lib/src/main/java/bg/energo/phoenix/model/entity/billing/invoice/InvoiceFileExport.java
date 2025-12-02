package bg.energo.phoenix.model.entity.billing.invoice;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Table(schema = "invoice", name = "invoice_file_export")
public class InvoiceFileExport extends BaseEntity {

    @Id
    @SequenceGenerator(schema = "invoice", allocationSize = 1, name = "invoice_file_export_id_seq", sequenceName = "invoice_file_export_id_seq")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "invoice_file_export_id_seq")
    private Long id;

    @Column(name = "billing_run_id")
    private Long billingRunId;

    @Column(name = "excel_path")
    private String excelPath;
    @Column(name = "file_name")
    private String fileName;


    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;


    public InvoiceFileExport(Long billingRunId, String excelPath, String fileName) {
        this.billingRunId = billingRunId;
        this.excelPath = excelPath;
        this.fileName = fileName;
        this.status = EntityStatus.ACTIVE;
    }
}
