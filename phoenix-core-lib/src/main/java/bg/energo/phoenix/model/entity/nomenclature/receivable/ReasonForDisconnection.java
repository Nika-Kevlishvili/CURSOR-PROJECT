package bg.energo.phoenix.model.entity.nomenclature.receivable;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.receivable.ReasonForDisconnectionRequest;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Setter
@Getter
@Table(name = "disconnection_reasons",schema = "nomenclature")
public class ReasonForDisconnection extends BaseEntity {
    @Id
    @SequenceGenerator(
            name = "disconnection_reasons_id_seq",
            sequenceName = "nomenclature.disconnection_reasons_id_seq",
            allocationSize = 1
    )
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE,generator = "disconnection_reasons_id_seq")
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "ordering_id")
    private Long orderingId;

    @Column(name = "is_default")
    private boolean defaultSelection;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private NomenclatureItemStatus status;

    public ReasonForDisconnection(ReasonForDisconnectionRequest request) {
        this.name=request.getName();
        this.status=request.getStatus();
        this.defaultSelection=request.getDefaultSelection();
    }

}
