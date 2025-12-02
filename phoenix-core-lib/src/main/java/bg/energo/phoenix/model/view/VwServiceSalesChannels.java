package bg.energo.phoenix.model.view;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Nationalized;

@Entity
@Table(name = "vw_service_sales_channels", schema = "service")
@Immutable
@Data
public class VwServiceSalesChannels {

    @Id
    private Long serviceDetailId;

    @Nationalized
    private String name;

    @Nationalized
    private String nameDesc;

}
