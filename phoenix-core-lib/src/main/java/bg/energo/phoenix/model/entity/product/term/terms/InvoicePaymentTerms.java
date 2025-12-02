package bg.energo.phoenix.model.entity.product.term.terms;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.product.term.terms.CalendarType;
import bg.energo.phoenix.model.enums.product.term.terms.DueDateChange;
import bg.energo.phoenix.model.enums.product.term.terms.PaymentTermStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor

@Table(name = "invoice_payment_terms", schema = "terms")
public class InvoicePaymentTerms extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "invoice_payment_terms_id_seq",
            sequenceName = "terms.invoice_payment_terms_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "invoice_payment_terms_id_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private CalendarType calendarType;

    @Column(name = "value")
    private Integer value;

    @Column(name = "value_from")
    private Integer valueFrom;

    @Column(name = "value_to")
    private Integer valueTo;

    @Column(name = "calendar_id")
    private Long calendarId;

    @Column(name = "exclude_weekends")
    private Boolean excludeWeekends;

    @Column(name = "exclude_holidays")
    private Boolean excludeHolidays;

    @Column(name = "due_date_change")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private DueDateChange dueDateChange;

    @Column(name = "term_id")
    private Long termId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private PaymentTermStatus status;

}