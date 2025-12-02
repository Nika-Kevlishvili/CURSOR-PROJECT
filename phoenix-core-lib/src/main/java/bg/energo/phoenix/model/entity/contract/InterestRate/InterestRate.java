package bg.energo.phoenix.model.entity.contract.InterestRate;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.contract.InterestRate.InterestRateCharging;
import bg.energo.phoenix.model.enums.contract.InterestRate.InterestRatePeriodicity;
import bg.energo.phoenix.model.enums.contract.InterestRate.InterestRateStatus;
import bg.energo.phoenix.model.enums.contract.InterestRate.InterestRateType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table(schema = "interest_rate", name = "interest_rates")
public class InterestRate extends BaseEntity {
    @Id
    @SequenceGenerator(
            name = "interest_ratess_seq",
            sequenceName = "interest_rate.interest_ratess_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "interest_ratess_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "is_default")
    private Boolean isDefault;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private InterestRateType type;

    @Column(name = "interest_charging")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private InterestRateCharging charging;

    @Column(name = "min_amount_for_interest_charging")
    private BigDecimal minAmountForInterestCharging;

    @Column(name = "min_amount_of_an_interest")
    private BigDecimal minAmountOfInterest;

    @Column(name = "max_amount_of_an_interest")
    private BigDecimal maxAmountOfInterest;

    @Column(name = "currency_id")
    private Long currencyId;

    @Column(name = "min_amount_of_interest_in_percent_of_the_liability")
    private BigDecimal minAmountOfInterestInPercentOfLiability;

    @Column(name = "max_amount_of_interest_in_percent_of_the_liability")
    private BigDecimal maxAmountOfInterestInPercentOfTheLiability;

    @Column(name = "grace_period")
    //private BigInteger gracePeriod;
    private Integer gracePeriod;

    @Column(name = "periodicity")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private InterestRatePeriodicity periodicity;

    @Column(name = "grouping_of_the_interest")
    private Boolean grouping;

    @Column(name = "income_account_number")
    private String incomeAccountNumber;

    @Column(name = "cost_center_controlling_order")
    private String costCenterControllingOrder;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private InterestRateStatus status;
}
