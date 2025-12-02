package bg.energo.phoenix.model.entity.nomenclature.customer;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.customer.EconomicBranchNCEARequest;
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

@Table(name = "economic_branch_ncea", schema = "nomenclature")
public class EconomicBranchNCEA extends BaseEntity {

    @Id
    @SequenceGenerator(
            name = "economic_branch_ncea_id_seq",
            sequenceName = "nomenclature.economic_branch_ncea_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "economic_branch_ncea_id_seq"
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
    private Boolean isDefault;

    public EconomicBranchNCEA(EconomicBranchNCEARequest request) {
        this.name = request.getName().trim();
        this.status = request.getStatus();
        this.isDefault = request.getDefaultSelection();
    }
}
