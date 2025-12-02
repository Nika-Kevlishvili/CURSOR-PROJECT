package bg.energo.phoenix.model.entity.receivable.collectionChannel;

import bg.energo.phoenix.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "payment_ftp_files", schema = "receivable")
public class PaymentFTPFiles extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "payment_ftp_files_id_seq",
            schema = "receivable",
            sequenceName = "payment_ftp_files_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "payment_ftp_files_id_seq"
    )
    private Long id;

    @Column(name = "file_name")
    private String name;
}
