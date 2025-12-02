package bg.energo.phoenix.model.entity.notification;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.service.notifications.enums.NotificationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_notifications", schema = "notification")
public class UserNotification extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_notifications_id_gen")
    @SequenceGenerator(name = "user_notifications_id_gen", schema = "notification", sequenceName = "user_notification_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "account_manager_id", nullable = false)
    private Long accountManagerId;

    @Column(name = "notification_type")
    @Enumerated(EnumType.STRING)
    private NotificationType notificationType;

    @Column(name = "is_readen")
    private Boolean isReaden;

    @Column(name = "read_date")
    private LocalDateTime readDate;

    @Column(name = "entity_id")
    private Long entityId;
}
