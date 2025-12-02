package bg.energo.phoenix.model.entity.nomenclature.contract;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.contract.ContractVersionTypesRequest;
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
@Table(schema = "nomenclature", name = "contract_version_types")
public class ContractVersionType extends BaseEntity {
    @Id
    @SequenceGenerator(
            name = "contract_version_types_seq",
            sequenceName = "nomenclature.contract_version_types_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "contract_version_types_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "is_default")
    private Boolean isDefault;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private NomenclatureItemStatus status;

    @Column(name = "ordering_id")
    private Long orderingId;

    @Column(name = "is_hard_coded")
    private Boolean isHardCoded;

    public ContractVersionType(ContractVersionTypesRequest request) {
        this.name = request.getName();
        this.isDefault = request.getDefaultSelection();
        this.status = request.getStatus();
    }
}
