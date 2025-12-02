package bg.energo.phoenix.model.entity.pod.discount.view;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "vw_discount_pods", schema = "pod")
@Immutable
@Data
public class VwDiscountPods {
    @Id
    @Column(name = "discount_id")
    private Long discountId;

    @Column(name = "name")
    private String name;

    @Column(name = "name_desc")
    private String nameDesc;

    @Column(name = "pod_identifier")
    private String podIdentifier;
}
