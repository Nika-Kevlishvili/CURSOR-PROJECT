package bg.energo.phoenix.model.response.product.goods;

import bg.energo.phoenix.model.enums.product.goods.GoodsDetailStatus;
import bg.energo.phoenix.model.enums.product.goods.GoodsStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GoodsListResponse {

    private Long id;

    private String name;

    private String groupName;

    private String supplierName;

    private GoodsDetailStatus goodsDetailStatus;

    private BigDecimal price;

    private String unitName;

    private String salesChannels;

    private GoodsStatus status;

    private Long goodsDetailsId;

    private LocalDateTime createdAt;

    private Boolean globalSalesChannel;

}
