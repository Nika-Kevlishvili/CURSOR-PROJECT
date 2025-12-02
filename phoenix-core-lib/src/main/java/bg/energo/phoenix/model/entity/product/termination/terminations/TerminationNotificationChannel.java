package bg.energo.phoenix.model.entity.product.termination.terminations;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.product.termination.terminations.TerminationNotificationChannelType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "termination_notification_channels", schema = "product")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class TerminationNotificationChannel extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "termination_notification_channels_id_seq",
            sequenceName = "product.termination_notification_channels_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "termination_notification_channels_id_seq"
    )
    private Long id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "notification_channel")
    private TerminationNotificationChannelType terminationNotificationChannelType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "termination_id", nullable = false)
    private Termination termination;

}
