package bg.energo.phoenix.model.entity.nomenclature.product;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Builder
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor

@Table(name = "grid_operators", schema = "nomenclature")
public class GridOperator extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "grid_operators_id_seq",
            sequenceName = "nomenclature.grid_operators_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "grid_operators_id_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "power_supply_termination_request_email")
    private String powerSupplyTerminationRequestEmail;

    @Column(name = "power_supply_reconnection_request_email")
    private String powerSupplyReconnectionRequestEmail;

    @Column(name = "objection_to_change_cbg_email")
    private String objectionToChangeCBGEmail;

    @Column(name = "code_for_xenergy")
    private String codeForXEnergy;

    @Column(name = "code")
    private String gridOperatorCode;

    @Column(name = "ordering_id")
    private Long orderingId;

    @Column(name = "is_default")
    private boolean defaultSelection;

    @Column(name = "is_owned_by_energo_pro")
    private Boolean ownedByEnergoPro;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private NomenclatureItemStatus status;

    @Column(name = "is_hard_coded")
    private Boolean isHardCoded;

}
