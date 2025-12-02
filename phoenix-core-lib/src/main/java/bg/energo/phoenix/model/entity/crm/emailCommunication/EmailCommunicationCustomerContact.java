package bg.energo.phoenix.model.entity.crm.emailCommunication;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.crm.emailCommunication.EmailCommunicationContactStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "email_communication_customer_contacts", schema = "crm")
public class EmailCommunicationCustomerContact extends BaseEntity {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "email_communication_customer_contacts_id_seq"
    )
    @SequenceGenerator(
            name = "email_communication_customer_contacts_id_seq",
            sequenceName = "crm.email_communication_customer_contacts_id_seq",
            allocationSize = 1
    )
    @Column(name = "id")
    private Long id;

    @Column(name = "customer_communication_contact_id")
    private Long customerCommunicationContactId;

    @Size(max = 128)
    @Column(name = "email_address", length = 128)
    private String emailAddress;

    @Column(name = "email_communication_customer_id")
    private Long emailCommunicationCustomerId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EmailCommunicationContactStatus status;

    @Size(max = 36)
    @Column(name = "task_id", length = 36)
    private String taskId;

}