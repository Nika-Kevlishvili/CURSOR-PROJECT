package bg.energo.phoenix.model.entity.receivable.rescheduling;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "rescheduling_reversal_transaction", schema = "receivable")
public class ReschedulingReversalTransaction {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "rescheduling_reversal_transaction_id_seq",
            sequenceName = "receivable.rescheduling_reversal_transaction_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "rescheduling_reversal_transaction_id_seq"
    )
    private Long id;

    @Column(name = "rescheduling_id")
    private Long reschedulingId;

    @Column(name = "transaction_id")
    private Long transactionId;
}
