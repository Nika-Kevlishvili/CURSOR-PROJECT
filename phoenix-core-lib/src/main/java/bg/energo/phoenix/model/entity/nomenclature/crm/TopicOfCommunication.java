package bg.energo.phoenix.model.entity.nomenclature.crm;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Builder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Getter
@Setter
@AllArgsConstructor
@Table(name = "communication_topics", schema = "nomenclature")
public class TopicOfCommunication extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "communication_topics_id_seq",
            sequenceName = "nomenclature.communication_topics_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "communication_topics_id_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "is_default")
    private boolean defaultSelection;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private NomenclatureItemStatus status;

    @Column(name = "ordering_id")
    private Long orderingId;

    @Column(name = "is_hard_coded")
    private Boolean isHardcoded;

}
