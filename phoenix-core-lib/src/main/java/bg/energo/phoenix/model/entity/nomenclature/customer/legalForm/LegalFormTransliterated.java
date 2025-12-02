package bg.energo.phoenix.model.entity.nomenclature.customer.legalForm;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.customer.legalForm.LegalFormTranRequest;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "legal_forms_transl",schema = "nomenclature")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class LegalFormTransliterated extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "legal_form_tr_seq",
            sequenceName = "nomenclature.legal_forms_transl_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "legal_form_tr_seq"
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

    @ManyToOne
    @JoinColumn(name = "legal_form_id")
    private LegalForm legalForm;

    public LegalFormTransliterated(LegalFormTranRequest request) {
        this.description = request.getDescription();
        this.name = request.getName();
    }
}
