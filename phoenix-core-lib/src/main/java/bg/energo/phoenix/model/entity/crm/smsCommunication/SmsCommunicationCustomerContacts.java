package bg.energo.phoenix.model.entity.crm.smsCommunication;

import bg.energo.phoenix.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "sms_communication_customer_contacts", schema = "crm")
public class SmsCommunicationCustomerContacts extends BaseEntity {

    @Id
    @SequenceGenerator(
            name = "sms_communication_customer_contacts_id_seq",
            sequenceName = "crm.sms_communication_customer_contacts_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "sms_communication_customer_contacts_id_seq")
    @Column(name = "id")
    private Long id;

    @Column(name = "customer_communication_contact_id")
    private Long customerCommunicationContactId;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "sms_communication_customer_id")
    private Long smsCommunicationCustomerId;

}
