package bg.energo.phoenix.model.entity.nomenclature.document;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "document_expiration_period", schema = "nomenclature", catalog = "phoenix")
public class DocumentExpirationPeriod extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "document_expiration_period_id_seq",
            sequenceName = "nomenclature.document_expiration_period_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "document_expiration_period_id_seq"
    )
    private Long id;

    @Column(name = "number_of_months")
    private Integer numberOfMonths;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

}
