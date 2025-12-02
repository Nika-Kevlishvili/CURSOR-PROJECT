package bg.energo.phoenix.model.entity.product.term.termsGroups;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.product.term.termsGroup.TermGroupStatus;
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

@Table(name = "term_groups", schema = "terms")
public class TermsGroups extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "groups_id_seq",
            sequenceName = "terms.groups_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "groups_id_seq"
    )
    private Long id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status")
    private TermGroupStatus status;

    @Column(name = "last_group_detail_id")
    private Long lastGroupDetailsId;

}
