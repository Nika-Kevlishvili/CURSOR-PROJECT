package bg.energo.phoenix.model.entity.pod.discount;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.pod.pod.PodStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Table(name = "discount_pods", schema = "pod")
public class DiscountPointOfDeliveries extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "discount_pods_seq")
    @SequenceGenerator(name = "discount_pods_seq", schema = "pod", sequenceName = "discount_pods_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "discount_id")
    private Long discountId;

    @Column(name = "pod_id")
    private Long pointOfDeliveryId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status")
    private PodStatus status;
}
