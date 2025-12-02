package phoenix.core.customer.model.entity.nomenclature.customer.legalForm;

import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import lombok.*;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import phoenix.core.customer.model.enums.nomenclature.NomenclatureItemStatus;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "legal_forms",schema = "nomenclature")
@NoArgsConstructor
@AllArgsConstructor
@TypeDef(
        name = "pgsql_enum",
        typeClass = PostgreSQLEnumType.class
)
@Getter
@Setter
@Builder
public class LegalForm {
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
    @Type(type = "pgsql_enum")
    private NomenclatureItemStatus status;

    @Column(name = "is_default")
    private Boolean defaultSelection;

    @Column(name = "ordering_id")
    private Long orderingId;

    @OneToMany(mappedBy = "legalForm",cascade = {CascadeType.PERSIST,CascadeType.MERGE})
    private List<LegalFormTransliterated> legalFormTransliterated;

    @Column(name = "system_user_id")
    private String systemUserId;

}
