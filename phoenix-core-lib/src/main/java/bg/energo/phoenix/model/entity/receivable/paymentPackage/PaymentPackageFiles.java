package bg.energo.phoenix.model.entity.receivable.paymentPackage;

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
@Entity
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table(name = "payment_packages_files", schema = "receivable")
public class PaymentPackageFiles extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "payment_packages_files_id_seq",
            schema = "receivable",
            sequenceName = "payment_packages_files_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "payment_packages_files_id_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "payment_packages_id")
    private Long paymentPackageId;

    @Column(name = "status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    private EntityStatus status;
}
