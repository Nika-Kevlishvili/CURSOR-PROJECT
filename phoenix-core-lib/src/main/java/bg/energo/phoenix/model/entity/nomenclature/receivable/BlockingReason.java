package bg.energo.phoenix.model.entity.nomenclature.receivable;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.receivable.massOperationForBlocking.ReceivableBlockingReasonType;
import bg.energo.phoenix.model.request.nomenclature.receivable.BlockingReasonRequest;
import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import io.hypersistence.utils.hibernate.type.array.internal.AbstractArrayType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "blocking_reasons", schema = "nomenclature")
public class BlockingReason extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "blocking_reasons_id_seq",
            sequenceName = "nomenclature.blocking_reasons_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "blocking_reasons_id_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "ordering_id")
    private Long orderingId;

    @Column(name = "is_default")
    private boolean defaultSelection;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private NomenclatureItemStatus status;

    @Column(name = "is_hard_coded")
    private boolean isHardCoded;

    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "nomenclature.blocking_reason_type"
            )
    )
    @Column(name = "reason_type", columnDefinition = "nomenclature.blocking_reason_type[]")
    private List<ReceivableBlockingReasonType> reasonTypes;

    public BlockingReason(BlockingReasonRequest request) {
        this.name = request.getName();
        this.status = request.getStatus();
        this.reasonTypes = request.getReasonTypes();
        this.defaultSelection = request.getDefaultSelection();
    }

}
