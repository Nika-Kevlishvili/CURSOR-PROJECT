package bg.energo.phoenix.model.entity.receivable.balancingGroupCoordinatorObjection;


import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "objection_to_change_of_cbg_doc_templates", schema = "receivable")
public class ObjectionToChangeOfCbgDocument extends BaseEntity {

    @Id
    @SequenceGenerator(
            name = "objection_to_change_of_cbg_doc_templates_id_seq",
            sequenceName = "receivable.objection_to_change_of_cbg_doc_templates_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "objection_to_change_of_cbg_doc_templates_id_seq")
    @Column(name = "id")
    private Long id;

    @Column(name = "change_of_cbg_id")
    private Long changeOfCbgId;

    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "name")
    private String fileName;

    @Column(name = "document_id")
    private Long documentId;
}
