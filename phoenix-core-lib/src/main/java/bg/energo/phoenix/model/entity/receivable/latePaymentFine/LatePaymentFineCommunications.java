package bg.energo.phoenix.model.entity.receivable.latePaymentFine;

import bg.energo.phoenix.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "late_payment_fine_communications", schema = "receivable")
@Builder
public class LatePaymentFineCommunications extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "late_payment_fine_communications_id_seq",
            sequenceName = "receivable.late_payment_fine_communications_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "late_payment_fine_communications_id_seq"
    )
    private Long id;

    @Column(name = "email_communication_id")
    private Long emailCommunicationId;

    @Column(name = "late_payment_fine_id")
    private Long latePaymentId;

}
