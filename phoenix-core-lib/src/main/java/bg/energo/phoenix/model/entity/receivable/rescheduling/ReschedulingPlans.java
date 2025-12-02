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
@Table(name = "rescheduling_plans", schema = "receivable")
public class ReschedulingPlans extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "rescheduling_plans_id_seq",
            sequenceName = "receivable.rescheduling_plans_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "rescheduling_plans_id_seq"
    )
    private Long id;

    @Column(name = "rescheduling_id")
    private Long reschedulingId;

    @Column(name = "installment_number")
    private String installmentNumber;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "principal_amount")
    private BigDecimal principalAmount;

    @Column(name = "interest_amount")
    private BigDecimal interestAmount;

    @Column(name = "fee")
    private BigDecimal fee;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "amount_without_interests")
    private BigDecimal amountWithoutInterests;
}
