package bg.energo.phoenix.model.entity.receivable.rescheduling;

import bg.energo.phoenix.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;


@Entity
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "rescheduling_draft_liabilities", schema = "receivable")
public class ReschedulingDraftLiabilities extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "rescheduling_draft_liabilities_id_seq",
            schema = "receivable",
            sequenceName = "rescheduling_draft_liabilities_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "rescheduling_draft_liabilities_id_seq"
    )
    private Long id;

    @Column(name = "rescheduling_id")
    private Long reschedulingId;

    @Column(name = "customer_liability_id")
    private Long customerLiabilityId;

}
