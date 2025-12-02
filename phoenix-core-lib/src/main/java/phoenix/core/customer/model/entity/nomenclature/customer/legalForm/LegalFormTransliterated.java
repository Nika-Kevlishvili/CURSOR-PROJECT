package phoenix.core.customer.model.entity.nomenclature.customer.legalForm;

import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import lombok.*;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import phoenix.core.customer.model.enums.nomenclature.NomenclatureItemStatus;

import javax.persistence.*;

@Entity
@Table(name = "legal_forms_transl",schema = "nomenclature")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@TypeDef(
        name = "pgsql_enum",
        typeClass = PostgreSQLEnumType.class
)
@Builder
public class LegalFormTransliterated {
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
    @Type(type = "pgsql_enum")
    private NomenclatureItemStatus status;

    @ManyToOne
    @JoinColumn(name = "legal_form_id")
    private LegalForm legalForm;

    @Column(name = "system_user_id")
    private String systemUserId;

}
