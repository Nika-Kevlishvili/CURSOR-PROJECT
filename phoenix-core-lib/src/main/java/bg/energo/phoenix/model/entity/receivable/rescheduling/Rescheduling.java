package bg.energo.phoenix.model.entity.receivable.rescheduling;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.rescheduling.ReschedulingInterestType;
import bg.energo.phoenix.model.enums.receivable.rescheduling.ReschedulingStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "reschedulings", schema = "receivable")
public class Rescheduling extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "reschedulings_id_seq",
            sequenceName = "receivable.reschedulings_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "reschedulings_id_seq"
    )
    private Long id;

    @Column(name = "rescheduling_number")
    private String reschedulingNumber;

    @Column(name = "customer_assessment_id")
    private Long customerAssessmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "rescheduling_status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ReschedulingStatus reschedulingStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "number_of_installment")
    private Integer numberOfInstallment;

    @Column(name = "amount_of_the_installment")
    private BigDecimal amountOfTheInstallment;

    @Column(name = "currency_id")
    private Long currencyId;

    @Column(name = "interest_rate_id")
    private Long interestRateId;

    @Column(name = "interest_rate_id_for_installments")
    private Long interestRateIdForInstallments;

    @Column(name = "installment_due_day")
    private Short installmentDueDay;

    @Enumerated(EnumType.STRING)
    @Column(name = "interest_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ReschedulingInterestType interestType;

    @Column(name = "customer_communication_id")
    private Long customerCommunicationId;

    @Column(name = "customer_communication_id_for_contract")
    private Long customerCommunicationIdForContract;

    @Column(name = "customer_detail_id")
    private Long customerDetailId;

    @Column(name = "reversed")
    private Boolean reversed;

    @Column(name = "initial_amount")
    private BigDecimal initialAmount;

    @Column(name = "current_amount")
    private BigDecimal currentAmount;

}
