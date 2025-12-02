package bg.energo.phoenix.model.entity.product.service;

import bg.energo.phoenix.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table(schema = "service", name = "service_additional_params")
public class ServiceAdditionalParams extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "service_additional_params_id_seq", sequenceName = "service.service_additional_params_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "service_additional_params_id_seq")
    private Long id;

    @Column(name = "ordering_id")
    private Long orderingId;

    @Column(name = "service_detail_id")
    private Long serviceDetailId;

    @Column(name = "label")
    private String label;

    @Column(name = "value")
    private String value;
}
