package bg.energo.phoenix.model.entity.product.service;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.product.service.ServiceAllowsSalesUnder;
import bg.energo.phoenix.model.enums.product.service.ServiceObligationCondition;
import bg.energo.phoenix.model.enums.product.service.ServiceSubobjectStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Objects;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor

@Table(schema = "service", name = "service_linked_services")
public class ServiceLinkedService extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "service_linked_services_id_seq",
            sequenceName = "service.service_linked_services_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "service_linked_services_id_seq"
    )
    private Long id;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "obligatory")
    private ServiceObligationCondition serviceObligationCondition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_detail_id", referencedColumnName = "id")
    private ServiceDetails serviceDetails;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_service_id", referencedColumnName = "id")
    private EPService service;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "allows_sales_under")
    private ServiceAllowsSalesUnder allowsSalesUnder;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ServiceSubobjectStatus status;

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id, serviceObligationCondition, service, allowsSalesUnder, status);
    }
}
