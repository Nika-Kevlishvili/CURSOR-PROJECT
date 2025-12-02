package bg.energo.phoenix.model.entity.billing.billingRun;

import bg.energo.phoenix.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "billing_invoice_sum_files", schema = "billing")
public class BillingSumFile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "billing_invoice_sum_files_id_seq")
    @SequenceGenerator(name = "billing_invoice_sum_files_id_seq", schema = "billing", sequenceName = "billing_invoice_sum_files_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "document_id")
    private Long documentId;

    @Column(name = "billing_id")
    private Long billingId;


}
