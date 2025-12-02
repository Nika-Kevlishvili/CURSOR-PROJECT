package bg.energo.phoenix.model.entity.nomenclature.pod;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.time.TimeZone;
import bg.energo.phoenix.model.request.nomenclature.pod.profiles.BaseProfilesRequest;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table(schema = "nomenclature", name = "profiles")
public class Profiles extends BaseEntity {
    @Id
    @SequenceGenerator(
            name = "profiles_seq",
            sequenceName = "nomenclature.profiles_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "profiles_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "is_default")
    private Boolean isDefault;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private NomenclatureItemStatus status;

    @Column(name = "ordering_id")
    private Long orderingId;

    @Column(name = "is_hard_coded")
    private Boolean isHardCoded;

    @Column(name = "timezone")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private TimeZone timeZone;

    public Profiles(BaseProfilesRequest request){
        this.name = request.getName();
        this.isDefault = request.getDefaultSelection();
        this.status = request.getStatus();
    }
}
