package bg.energo.phoenix.model.entity.receivable.balancingGroupCoordinatorObjection;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.shared.DocumentFileStatus;
import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import io.hypersistence.utils.hibernate.type.array.internal.AbstractArrayType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "objection_to_change_of_cbg_sub_files", schema = "receivable")
public class ObjectionToChangeOfCbgSubFiles extends BaseEntity {
    @SequenceGenerator(
            name = "objection_to_change_of_cbg_sub_files_id_seq",
            sequenceName = "objection_to_change_of_cbg_sub_files_id_seq",
            schema = "receivable",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "objection_to_change_of_cbg_sub_files_id_seq"
    )
    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "obj_to_change_of_cbg_id")
    private Long objToChangeOfCbgId;

    @Column(name = "status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "receivable.file_status"
            )
    )
    @Column(name = "file_statuses", columnDefinition = "receivable.file_status[]")
    private List<DocumentFileStatus> fileStatuses;

}
