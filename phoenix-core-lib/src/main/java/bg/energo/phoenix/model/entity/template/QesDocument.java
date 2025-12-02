package bg.energo.phoenix.model.entity.template;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.template.QesSigningStatus;
import bg.energo.phoenix.model.enums.template.QesStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "qes_documents", schema = "template")
public class QesDocument extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "qes_documents_id_gen")
    @SequenceGenerator(name = "qes_documents_id_gen", sequenceName = "qes_documents_id_seq",schema="template", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;



    @Column(name = "document_id")
    private Long document_id;

    @Column(name = "identifier", length = 512)
    private String identifier;

    @Column(name = "quantity_to_sign")
    private Integer quantityToSign;

    @Column(name = "signed_quantity")
    private Integer signedQuantity;



    @Column(name = "signing_status", columnDefinition = "qes_document_signing_status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private QesSigningStatus signingStatus;


    @Column(name = "status", columnDefinition = "qes_document_status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private QesStatus status;
}