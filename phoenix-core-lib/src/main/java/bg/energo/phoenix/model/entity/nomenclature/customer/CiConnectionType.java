package bg.energo.phoenix.model.entity.nomenclature.customer;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.customer.CiConnectionTypeRequest;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor

@Table(name = "connection_types_rci", schema = "nomenclature")
public class CiConnectionType extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "connection_types_rci_seq",
            sequenceName = "nomenclature.connection_types_rci_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "connection_types_rci_seq"
    )
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

    public CiConnectionType(CiConnectionTypeRequest request) {
        this.name = request.getName().trim();
        this.defaultSelection = request.getDefaultSelection();
        this.status = request.getStatus();
    }
}
