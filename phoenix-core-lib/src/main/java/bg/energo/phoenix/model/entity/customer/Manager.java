package bg.energo.phoenix.model.entity.customer;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.nomenclature.customer.RepresentationMethod;
import bg.energo.phoenix.model.entity.nomenclature.customer.Title;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.request.customer.manager.CreateManagerRequest;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor

@Table(name = "customer_managers", schema = "customer")
public class Manager extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "customer_managers_seq",
            sequenceName = "customer.customer_managers_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "customer_managers_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "middle_name")
    private String middleName;

    @Column(name = "surname")
    private String surname;

    @Column(name = "personal_number")
    private String personalNumber;

    @Column(name = "job_position")
    private String jobPosition;

    @Column(name = "position_held_from")
    private LocalDate positionHeldFrom;

    @Column(name = "position_held_to")
    private LocalDate positionHeldTo;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "representation_method_id", nullable = false)
    private RepresentationMethod representationMethod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "title_id", nullable = false)
    private Title title;

    @Column(name = "additional_info")
    private String additionalInfo;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private Status status;

    @Column(name = "customer_detail_id")
    private Long customerDetailId;

    public Manager(CreateManagerRequest request, Long customerDetailId) {
        this.name = request.getName();
        this.middleName = request.getMiddleName();
        this.surname = request.getSurname();
        this.personalNumber = request.getPersonalNumber();
        this.jobPosition = request.getJobPosition();
        this.positionHeldFrom = request.getPositionHeldFrom();
        this.positionHeldTo = request.getPositionHeldTo();
        this.birthDate = request.getBirthDate()==null  ? null : LocalDate.parse(request.getBirthDate());
        this.additionalInfo = request.getAdditionalInformation();
        this.status = request.getStatus();
        this.customerDetailId = customerDetailId;
    }
}
