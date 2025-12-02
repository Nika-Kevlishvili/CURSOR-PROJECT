package bg.energo.phoenix.model.entity.nomenclature.contract.subActivity;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.nomenclature.contract.Activity;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.contract.SubActivityRequest;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "sub_activity", schema = "nomenclature")
public class SubActivity extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "sub_activity_seq",
            sequenceName = "nomenclature.sub_activity_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "sub_activity_seq"
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @Column(name = "name")
    private String name;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private NomenclatureItemStatus status;

    @Column(name = "is_default")
    private boolean defaultSelection;

    @Column(name = "ordering_id")
    private Long orderingId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "fields", columnDefinition = "jsonb")
    private List<SubActivityJsonField> fields;

    public SubActivity(SubActivityRequest request) {
        this.name = request.getName();
        this.status = request.getStatus();
        this.fields = request.getFields();
        this.defaultSelection = request.getDefaultSelection();
    }
}
