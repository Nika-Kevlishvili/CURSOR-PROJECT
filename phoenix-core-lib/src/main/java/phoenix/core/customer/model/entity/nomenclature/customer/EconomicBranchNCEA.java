package phoenix.core.customer.model.entity.nomenclature.customer;

import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import phoenix.core.customer.model.enums.nomenclature.NomenclatureItemStatus;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@TypeDef(
        name = "pgsql_enum",
        typeClass = PostgreSQLEnumType.class
)
@Table(name = "economic_branch_ncea", schema = "nomenclature")
@Builder
public class EconomicBranchNCEA {

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
    @Type(type = "pgsql_enum")
    private NomenclatureItemStatus status;

    @Column(name = "ordering_id")
    private Long orderingId;

    @Column(name = "is_default")
    private Boolean isDefault;

    @Column(name = "system_user_id")
    private String systemUserId;

}
