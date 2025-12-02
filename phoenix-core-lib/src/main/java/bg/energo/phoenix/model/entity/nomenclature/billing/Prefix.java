package bg.energo.phoenix.model.entity.nomenclature.billing;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.billing.PrefixRequest;
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
@Table(name = "prefixes", schema = "nomenclature")
public class Prefix extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "nomenclature_prefix_id_seq",
            schema = "nomenclature",
            sequenceName = "prefixes_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "nomenclature_prefix_id_seq"
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
    private boolean isDefault;

    @Column(name = "is_hard_coded")
    private Boolean isHardCoded;

    @Column(name = "prefix_type")
    private String prefixType;

    public Prefix(PrefixRequest prefixRequest) {
        this.name = prefixRequest.getName().trim();
        this.status = prefixRequest.getStatus();
        this.isDefault = prefixRequest.getDefaultSelection();
    }
}
