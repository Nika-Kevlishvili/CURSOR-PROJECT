package bg.energo.phoenix.model.entity.contract.InterestRate;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.contract.InterestRate.InterestRatePeriodStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table(schema = "interest_rate", name = "interest_rates_and_periods")
public class InterestRatePeriods extends BaseEntity {
    @Id
    @SequenceGenerator(
            name = "interest_rate_rates_and_periods_id_seq",
            sequenceName = "interest_rate.interest_rate_rates_and_periods_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "interest_rate_rates_and_periods_id_seq"
    )
    private Long id;

    @Column(name = "amount_in_percent")
    private BigDecimal amountInPercent;

    @Column(name = "base_interest_rate")
    private BigDecimal baseInterestRate;

    @Column(name = "applicable_interest_rate")
    private BigDecimal applicableInterestRate;

    @Column(name = "fee")
    private BigDecimal fee;

    @Column(name = "currency_id")
    private Long currencyId;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private InterestRatePeriodStatus status;

    @Column(name = "interest_rate_id")
    private Long interestRateId;

}
