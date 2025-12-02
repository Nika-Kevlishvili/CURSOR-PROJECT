package bg.energo.phoenix.service.signing.qes.entities;

import bg.energo.phoenix.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "qes_document_details",schema = "template")
public class QesDocumentDetails extends BaseEntity {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "qes_document_details_id_seq"
    )
    @SequenceGenerator(
            name = "qes_document_details_id_seq",
            sequenceName = "template.qes_document_details_id_seq",
            allocationSize = 1
    )
    private Long id;

    private Long qesDocumentId;

    @Column(name = "process_identifier")
    private String processId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private QesDocumentDetailsStatus status;

    private String ftpPath;
    private boolean isActive;

}
