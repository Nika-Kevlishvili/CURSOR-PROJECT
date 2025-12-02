package bg.energo.phoenix.model.entity.receivable.balancingGroupCoordinatorObjection;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.balancingGroupCoordinatorObjection.ChangeOfCbgStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "objection_to_change_of_cbg", schema = "receivable")
public class ObjectionToChangeOfCbg extends BaseEntity {

    @Id
    @SequenceGenerator(
            name = "objection_to_change_of_cbg_id_seq",
            sequenceName = "receivable.objection_to_change_of_cbg_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "objection_to_change_of_cbg_id_seq")
    @Column(name = "id")
    private Long id;

    @Column(name = "change_of_cbg_number")
    private String changeOfCbgNumber;

    @Column(name = "grid_operator_id")
    private Long gridOperatorId;

    @Column(name = "change_date")
    private LocalDate changeDate;

    @Column(name = "change_of_cbg_status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ChangeOfCbgStatus changeOfCbgStatus;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

    @Column(name = "email_template_id")
    private Long emailTemplateId;
}
