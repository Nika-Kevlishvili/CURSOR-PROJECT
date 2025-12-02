package bg.energo.phoenix.model.entity.product.product.view;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Nationalized;

@Entity
@Table(name = "vw_product_sales_channels", schema = "product")
@Immutable
@Data
public class VwProductSalesChannels {
    @Id
    private Long productDetailId;

    @Nationalized
    private String name;

    @Nationalized
    private String nameDesc;
}
