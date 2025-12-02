package bg.energo.phoenix.model.entity.nomenclature.customer;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.customer.UnwantedCustomerReasonRequest;
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

@Table(name = "unwanted_customers_reasons", schema = "nomenclature")
public class UnwantedCustomerReason extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "unwanted_customers_reasons_seq",
            sequenceName = "nomenclature.unwanted_customers_reasons_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "unwanted_customers_reasons_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private NomenclatureItemStatus status;

    @Column(name = "ordering_id")
    private Long orderingId;

    @Column(name = "is_default")
    private boolean defaultSelection;

    public UnwantedCustomerReason(UnwantedCustomerReasonRequest unwantedCustomerReasonRequest) {
        this.name = unwantedCustomerReasonRequest.getName().trim();
        this.status = unwantedCustomerReasonRequest.getStatus();
        this.defaultSelection = unwantedCustomerReasonRequest.getDefaultSelection();
    }
}
