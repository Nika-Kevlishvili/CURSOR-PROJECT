package bg.energo.migration.customer.entity.manager;

import bg.energo.migration.integration.phoenix.customer.model.enums.Status;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Immutable;

@Data
@Entity
@Immutable
@Table(name = "v_customer_managers", schema = "customer_migration")
public class CustomerManagerView {

    @Id
    private Long id;

    @Column(name = "customer_id")
    private String customerId;

    @Column(name = "personal_number")
    private String personalNumber;

    @Column(name = "name")
    private String name;

    @Column(name = "middle_name")
    private String middleName;

    @Column(name = "surname")
    private String surname;

    @Column(name = "job_position")
    private String jobPosition;

    @Column(name = "title_id")
    private Long titleId;

    @Column(name = "representation_method_id")
    private Long representationMethodId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private Status status;

}
