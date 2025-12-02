package bg.energo.phoenix.model.entity.contract.InterestRate;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.contract.InterestRate.InterestRatePaymentTermsCalendarType;
import bg.energo.phoenix.model.enums.contract.InterestRate.InterestRateSubObjectStatus;
import bg.energo.phoenix.model.enums.contract.InterestRate.InterestRateTermsDueDateChange;
import bg.energo.phoenix.model.enums.contract.InterestRate.InterestRateTermsExclude;
import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import io.hypersistence.utils.hibernate.type.array.internal.AbstractArrayType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table(schema = "interest_rate", name = "interest_rate_payment_terms")

public class InterestRatePaymentTerms extends BaseEntity {
    @Id
    @SequenceGenerator(
            name = "interest_rate_payment_terms_seq",
            sequenceName = "interest_rate.interest_rate_payment_terms_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "interest_rate_payment_terms_seq"
    )
    private Long id;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private InterestRatePaymentTermsCalendarType type;

    @Column(name = "value")
    private Integer value;

    @Column(name = "value_from")
    private Integer valueFrom;

    @Column(name = "value_to")
    private Integer valueTo;

    @Column(name = "calendar_id")
    private Long calendarId;

    @Column(name = "due_date_change")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private InterestRateTermsDueDateChange dueDateChange;

    @Column(name = "name")
    private String name;

    @Column(name = "interest_rate_id")
    private Long interestRateId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private InterestRateSubObjectStatus status;

    @Column(name = "excludes", columnDefinition = "interest_rate.interest_rate_exclude[]")
    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "interest_rate.interest_rate_exclude"
            )
    )
    private List<InterestRateTermsExclude> excludes;


}
