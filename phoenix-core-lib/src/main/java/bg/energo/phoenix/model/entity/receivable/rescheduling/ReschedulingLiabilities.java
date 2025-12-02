package bg.energo.phoenix.model.entity.receivable.rescheduling;

import bg.energo.phoenix.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "rescheduling_liabilities", schema = "receivable")

public class ReschedulingLiabilities extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "rescheduling_liabilities_id_seq",
            sequenceName = "receivable.rescheduling_liabilities_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "rescheduling_liabilities_id_seq"
    )
    private Long id;

    @Column(name = "customer_liabilitie_id")
    private Long customerLiabilitieId;

    @Column(name = "rescheduling_id")
    private Long reschedulingId;

    @Column(name = "current_amount")
    private BigDecimal currentAmount;

    @Column(name = "interests_from_date")
    private LocalDate interestsFromDate;
}
