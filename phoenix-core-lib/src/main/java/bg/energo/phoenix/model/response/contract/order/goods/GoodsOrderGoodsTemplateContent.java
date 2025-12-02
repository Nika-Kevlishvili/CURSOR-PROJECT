package bg.energo.phoenix.model.response.contract.order.goods;

import org.springframework.core.io.ByteArrayResource;

public record GoodsOrderGoodsTemplateContent(String fileName, ByteArrayResource content) {

}
