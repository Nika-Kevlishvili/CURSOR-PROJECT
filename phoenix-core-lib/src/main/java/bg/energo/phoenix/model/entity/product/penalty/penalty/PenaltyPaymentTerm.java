package bg.energo.phoenix.model.entity.product.penalty.penalty;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.product.term.terms.CalendarType;
import bg.energo.phoenix.model.enums.product.term.terms.DueDateChange;
import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import io.hypersistence.utils.hibernate.type.array.internal.AbstractArrayType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "penalty_payment_terms", schema = "terms")
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PenaltyPaymentTerm extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "penalty_payment_terms_id_seq",
            sequenceName = "terms.penalty_payment_terms_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "penalty_payment_terms_id_seq"
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

    @Column(name = "due_date_change")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private DueDateChange dueDateChange;

    @Column(name = "penalty_id")
    private Long penaltyId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "terms.penalty_payment_term_exclude"
            )
    )
    @Column(name = "excludes", columnDefinition = "terms.penalty_payment_term_exclude[]")
    private List<PenaltyPaymentTermExclude> penaltyPaymentTermExcludes;

}
