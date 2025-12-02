package bg.energo.phoenix.model.entity.nomenclature.billing;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.nomenclature.DefaultAssignmentType;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.billing.IncomeAccountNameRequest;
import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import io.hypersistence.utils.hibernate.type.array.internal.AbstractArrayType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor

@Table(name = "income_account", schema = "nomenclature")
public class IncomeAccountName extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "income_account_id_seq",
            sequenceName = "nomenclature.income_account_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "income_account_id_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "number")
    private String number;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private NomenclatureItemStatus status;

    @Column(name = "ordering_id")
    private Long orderingId;

    @Column(name = "is_default")
    private boolean defaultSelection;

    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "nomenclature.default_assignment_type"
            )
    )
    @Column(name = "default_assignment_type", columnDefinition = "nomenclature.default_assignment_type[]")
    private List<DefaultAssignmentType> defaultAssignmentType;

    public IncomeAccountName(IncomeAccountNameRequest request) {
        this.name = request.getName().trim();
        this.number = request.getNumber().trim();
        this.status = request.getStatus();
        this.defaultAssignmentType = request.getDefaultAssignmentType();
    }

}
