package bg.energo.phoenix.model.entity.nomenclature.product.service;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.product.service.ServiceTypeRequest;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor

@Table(name = "service_types", schema = "nomenclature")
public class ServiceType extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "service_types_id_seq",
            sequenceName = "nomenclature.service_types_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "service_types_id_seq"
    )
    private Long id;

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

    public ServiceType(ServiceTypeRequest request) {
        this.name = request.getName().trim();
        this.status = request.getStatus();
        this.defaultSelection =request.getStatus().equals(NomenclatureItemStatus.ACTIVE) && request.getDefaultSelection();
    }
}
