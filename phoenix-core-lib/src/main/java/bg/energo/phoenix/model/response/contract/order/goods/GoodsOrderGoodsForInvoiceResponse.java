package bg.energo.phoenix.model.response.contract.order.goods;

import java.math.BigDecimal;

public interface GoodsOrderGoodsForInvoiceResponse {
    Long getId();
    Integer getQuantity();
    BigDecimal getPrice();
    Long getCurrencyId();
    Long getAltCurrencyId();
    BigDecimal getAltCurrencyExchangeRate();
    String getNumberOfIncomingAccount();
    String getCostCenterOrControllingOrder();
    Long getGoodsUnitId();
    String getName();
}
