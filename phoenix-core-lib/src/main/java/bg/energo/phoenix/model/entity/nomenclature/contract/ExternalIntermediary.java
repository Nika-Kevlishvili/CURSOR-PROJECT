package bg.energo.phoenix.model.entity.nomenclature.contract;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.contract.ExternalIntermediaryRequest;
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

@Table(name = "external_intermediaries", schema = "nomenclature")
public class ExternalIntermediary extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "external_intermediaries_seq",
            sequenceName = "nomenclature.external_intermediaries_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "external_intermediaries_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "identifier")
    private String identifier;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private NomenclatureItemStatus status;

    @Column(name = "ordering_id")
    private Long orderingId;

    @Column(name = "is_default")
    private boolean defaultSelection;

    public ExternalIntermediary(ExternalIntermediaryRequest request) {
        this.name = request.getName().trim();
        this.identifier = request.getIdentifier();
        this.status = request.getStatus();
        this.defaultSelection = request.getDefaultSelection();
    }
}
