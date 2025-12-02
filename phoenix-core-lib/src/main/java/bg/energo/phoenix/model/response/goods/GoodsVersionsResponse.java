package bg.energo.phoenix.model.response.goods;

import bg.energo.phoenix.model.enums.product.goods.GoodsDetailStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GoodsVersionsResponse {
    private Long version;
    private GoodsDetailStatus status;
    private LocalDateTime createDate;
}
