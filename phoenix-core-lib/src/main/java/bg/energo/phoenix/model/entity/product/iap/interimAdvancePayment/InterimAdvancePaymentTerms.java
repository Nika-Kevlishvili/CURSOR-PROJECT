package bg.energo.phoenix.model.entity.product.iap.interimAdvancePayment;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.nomenclature.product.terms.Calendar;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.InterimAdvancePaymentSubObjectStatus;
import bg.energo.phoenix.model.enums.product.term.terms.CalendarType;
import bg.energo.phoenix.model.enums.product.term.terms.DueDateChange;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(schema = "interim_advance_payment", name = "interim_advance_payment_terms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)

public class InterimAdvancePaymentTerms extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "interim_advance_payment_terms_id_seq",
            sequenceName = "interim_advance_payment.interim_advance_payment_terms_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "interim_advance_payment_terms_id_seq"
    )
    private Long id;

    @ManyToOne
    @JoinColumn(name = "interim_advance_payment_id")
    private InterimAdvancePayment interimAdvancePayment;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private CalendarType calendarType;

    @ManyToOne
    @JoinColumn(name = "calendar_id")
    private Calendar calendar;

    @Column(name = "value")
    private Integer value;

    @Column(name = "value_from")
    private Integer valueFrom;

    @Column(name = "value_to")
    private Integer valueTo;

    @Column(name = "exclude_weekends")
    private Boolean excludeWeekends;

    @Column(name = "exclude_holidays")
    private Boolean excludeHolidays;

    @Column(name = "due_date_change")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private DueDateChange dueDateChange;

    @Column(name = "name")
    private String name;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private InterimAdvancePaymentSubObjectStatus status;

}
