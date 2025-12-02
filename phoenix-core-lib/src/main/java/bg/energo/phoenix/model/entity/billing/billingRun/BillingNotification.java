package bg.energo.phoenix.model.entity.billing.billingRun;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.task.PerformerType;
import bg.energo.phoenix.model.request.billing.billingRun.BillingNotificationType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "billing_notifications", schema = "billing")
public class BillingNotification extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "billing_notifications_id_seq")
    @SequenceGenerator(name = "billing_notifications_id_seq", sequenceName = "billing.billing_notifications_id_seq", allocationSize = 1)
    private Long id;

    @NotNull
    @Column(name = "billing_id", nullable = false)
    private Long billing;

    @Column(name = "employee_id")
    private Long employee;

    @Column(name = "tag_id")
    private Long tag;


    @Column(name = "performer_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private PerformerType performerType;
    @Column(name = "notification_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private BillingNotificationType type;
}