package bg.energo.phoenix.model.entity.nomenclature.customer;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.customer.MissingCustomerRequest;
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
@Table(name = "missing_customer", schema = "nomenclature")
public class MissingCustomer extends BaseEntity {

    @Id
    @SequenceGenerator(
            name = "missing_customer_id_seq",
            sequenceName = "nomenclature.missing_customer_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "missing_customer_id_seq"
    )
    private Long id;

    @Column(name = "uic")
    private String uic;

    @Column(name = "name")
    private String name;

    @Column(name = "name_transliterated")
    private String nameTransliterated;

    @Column(name = "legal_form")
    private String legalForm;

    @Column(name = "legal_form_transliterated")
    private String legalFormTransliterated;

    @Column(name = "is_default")
    private Boolean isDefault;

    @Column(name = "ordering_id")
    private Long orderingId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private NomenclatureItemStatus status;

    public MissingCustomer(MissingCustomerRequest request){
        this.uic = request.getUic();
        this.name = request.getName();
        this.nameTransliterated = request.getNameTransliterated();
        this.legalForm = request.getLegalForm();
        this.legalFormTransliterated = request.getLegalFormTransliterated();
        this.isDefault = request.getDefaultSelection();
        this.status = request.getStatus();
    }

}
