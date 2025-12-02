package bg.energo.phoenix.model.entity.nomenclature.receivable;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table(name = "grounds_for_objection_withdrawal_to_change_of_cbg", schema = "nomenclature")
public class GroundForObjectionWithdrawalToChangeOfACbg extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "grounds_for_objection_withdrawal_to_change_of_cbg_id_seq",
            sequenceName = "nomenclature.grounds_for_objection_withdrawal_to_change_of_cbg_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "grounds_for_objection_withdrawal_to_change_of_cbg_id_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private NomenclatureItemStatus status;

    @Column(name = "ordering_id")
    private Long orderingId;

    @Column(name = "is_default")
    private boolean defaultSelection;
}
