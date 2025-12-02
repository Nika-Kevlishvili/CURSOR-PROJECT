package bg.energo.phoenix.service.signing.qes.entities;

import bg.energo.phoenix.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "qes_sign_session_objects",schema = "template")
public class QesSignSessionObjects extends BaseEntity {
    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "qes_sign_session_objects_id_seq"
    )
    @SequenceGenerator(
            name = "qes_sign_session_objects_id_seq",
            sequenceName = "template.qes_sign_session_objects_id_seq",
            allocationSize = 1
    )
    private Long id;

    private Long qesSignSessionId;

    @ManyToOne
    @JoinColumn(name = "qes_document_detail_id")
    private QesDocumentDetails qesDocumentDetails;
}
