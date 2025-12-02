package bg.energo.phoenix.model.entity.contract.action;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "action_signable_documents", schema = "action")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActionSignableDocuments extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "action_signable_documents_id_seq")
    @SequenceGenerator(name = "action_signable_documents_id_seq", schema = "action", sequenceName = "action_signable_documents_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "document_id")
    private Long documentId;

    @Column(name = "service_contract_detail_id")
    private Long serviceContractDetailId;

    @Column(name = "product_contract_detail_id")
    private Long productContractDetailId;

    @Column(name = "action_id")
    private Long actionId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

}
