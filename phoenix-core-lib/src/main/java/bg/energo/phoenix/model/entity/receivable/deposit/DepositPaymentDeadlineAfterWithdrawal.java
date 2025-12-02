package bg.energo.phoenix.model.entity.receivable.deposit;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.product.term.terms.CalendarType;
import bg.energo.phoenix.model.enums.product.term.terms.DueDateChange;
import bg.energo.phoenix.model.enums.receivable.deposit.DepositPaymentDeadlineExclude;
import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import io.hypersistence.utils.hibernate.type.array.internal.AbstractArrayType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "customer_deposit_payment_ddl_aft_withdrawal", schema = "receivable")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class DepositPaymentDeadlineAfterWithdrawal extends BaseEntity {

    @Id
    @SequenceGenerator(
            name = "customer_deposit_payment_deadline_after_withdrawal_id_seq",
            sequenceName = "receivable.customer_deposit_payment_deadline_after_withdrawal_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "customer_deposit_payment_deadline_after_withdrawal_id_seq")
    @Column(name = "id")
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

    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "receivable.customer_deposit_payment_deadline_change_to"
            )
    )
    @Column(name = "change_to", columnDefinition = "customer_deposit_payment_deadline_change_to[]")
    private List<DueDateChange> dueDateChange;

    @Column(name = "customer_deposit_id")
    private Long depositId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "receivable.customer_deposit_payment_deadline_exclude"
            )
    )
    @Column(name = "excludes", columnDefinition = "receivable.customer_deposit_payment_deadline_exclude[]")
    private List<DepositPaymentDeadlineExclude> depositPaymentDeadlineExcludes;
}
