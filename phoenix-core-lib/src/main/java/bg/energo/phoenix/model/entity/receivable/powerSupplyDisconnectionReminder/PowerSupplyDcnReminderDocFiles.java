package bg.energo.phoenix.model.entity.receivable.powerSupplyDisconnectionReminder;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "power_supply_disconnection_reminder_doc_files", schema = "receivable")
public class PowerSupplyDcnReminderDocFiles extends BaseEntity {

    @Id
    @SequenceGenerator(
            name = "power_supply_disconnection_reminder_doc_files_id_seq",
            sequenceName = "receivable.power_supply_disconnection_reminder_doc_files_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "power_supply_disconnection_reminder_doc_files_id_seq")
    @Column(name = "id")
    private Long id;

    @Column(name = "reminder_for_disconnection_id")
    private Long reminderForDcnId;

    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "name")
    private String fileName;

    @Column(name = "customer_id")
    private Long customerId;

}
