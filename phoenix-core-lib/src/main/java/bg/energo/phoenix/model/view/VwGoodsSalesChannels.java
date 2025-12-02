package bg.energo.phoenix.model.view;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Nationalized;

@Entity
@Table(name = "vw_goods_sales_channels", schema = "goods")
@Immutable
@Data
public class VwGoodsSalesChannels {

    @Id
    private Long goodsDetailsId;

    @Nationalized
    private String salesChannelsName;

    @Nationalized
    private String salesChannelsNameDesc;

}
