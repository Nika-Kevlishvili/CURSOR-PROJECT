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

@Table(name = "term_group_terms", schema = "terms")
public class TermsGroupTerms extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "term_group_terms_id_seq",
            sequenceName = "terms.term_group_terms_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "term_group_terms_id_seq"
    )
    private Long id;

    @Column(name = "term_group_detail_id")
    private Long termGroupDetailId;

    @Column(name = "term_id")
    private Long termId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status")
    private TermGroupStatus termGroupStatus;
}
