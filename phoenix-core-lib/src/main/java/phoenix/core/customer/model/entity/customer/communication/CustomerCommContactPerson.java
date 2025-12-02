package phoenix.core.customer.model.entity.customer.communication;

import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import phoenix.core.customer.model.entity.BaseEntity;
import phoenix.core.customer.model.enums.customer.Status;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "customer_comm_contact_persons", schema = "customer")
@TypeDef(
        name = "pgsql_enum",
        typeClass = PostgreSQLEnumType.class
)
@Data
@EqualsAndHashCode(callSuper = true)
public class CustomerCommContactPerson extends BaseEntity {

    @Id
    @SequenceGenerator(
            name = "customer_comm_contact_persons_seq",
            sequenceName = "customer.customer_comm_contact_persons_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "customer_comm_contact_persons_seq"
    )
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", length = 512, nullable = false)
    private String name;

    @Column(name = "middle_name", length = 512)
    private String middleName;

    @Column(name = "surname", length = 512, nullable = false)
    private String surname;

    @Column(name = "job_position", length = 512, nullable = false)
    private String jobPosition;

    @Column(name = "position_held_from")
    private LocalDate positionHeldFrom;

    @Column(name = "position_held_to")
    private LocalDate positionHeldTo;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "additional_info", length = 2048)
    private String additionalInfo;

    @Column(name = "title_id", nullable = false)
    private Long titleId;

    @Column(name = "status", nullable = false)
    @Type(type = "pgsql_enum")
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "customer_communication_id")
    private Long customerCommunicationsId;

}
