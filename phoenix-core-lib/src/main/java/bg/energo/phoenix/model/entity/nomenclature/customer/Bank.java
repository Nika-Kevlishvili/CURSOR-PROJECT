package bg.energo.phoenix.model.entity.nomenclature.customer;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.customer.BankRequest;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(name = "banks", schema = "nomenclature")
public class Bank extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "banks_seq",
            sequenceName = "nomenclature.banks_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "banks_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "bic")
    private String bic;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private NomenclatureItemStatus status;

    @Column(name = "ordering_id")
    private Long orderingId;

    @Column(name = "is_default")
    private boolean defaultSelection;

    public Bank(BankRequest request) {
        this.name = request.getName().trim();
        this.bic = request.getBic().trim();
        this.status = request.getStatus();
        this.defaultSelection = request.getDefaultSelection();
    }
}
