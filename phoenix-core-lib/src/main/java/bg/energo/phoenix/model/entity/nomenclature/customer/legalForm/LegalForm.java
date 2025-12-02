package bg.energo.phoenix.model.entity.nomenclature.customer.legalForm;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.customer.legalForm.CreateLegalFormRequest;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "legal_forms",schema = "nomenclature")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class LegalForm extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "legal_form_seq",
            sequenceName = "nomenclature.legal_forms_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "legal_form_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "full_descr")
    private String description;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private NomenclatureItemStatus status;

    @Column(name = "is_default")
    private Boolean defaultSelection;

    @Column(name = "ordering_id")
    private Long orderingId;

    @OneToMany(mappedBy = "legalForm",cascade = {CascadeType.PERSIST,CascadeType.MERGE})
    private List<LegalFormTransliterated> legalFormTransliterated;

    public LegalForm(CreateLegalFormRequest request){
        this.description = request.getDescription().trim();
        this.name = request.getName().trim();
        this.defaultSelection = request.getDefaultSelection();
        this.status = request.getStatus();
    }
}
