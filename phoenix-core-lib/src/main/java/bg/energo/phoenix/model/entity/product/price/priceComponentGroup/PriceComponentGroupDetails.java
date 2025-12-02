package bg.energo.phoenix.model.entity.product.price.priceComponentGroup;

import bg.energo.phoenix.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor

@Table(name = "price_component_group_details", schema = "price_component")
public class PriceComponentGroupDetails extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "price_component_group_details_id_seq",
            sequenceName = "price_component.price_component_group_details_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "price_component_group_details_id_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "price_component_group_id")
    private Long priceComponentGroupId;

    @Column(name = "version_id")
    private Long versionId;

}
