package bg.energo.phoenix.model.response.contract.order.goods;

import java.math.BigDecimal;

public record GoodsOrderForInvoiceResponse(
        Long id,
        String incomeAccountNumber,
        String costCenterControllingOrder,
        Long applicableInterestRateId,
        Boolean directDebit,
        Long bankId,
        String iban,
        Long customerDetailId,
        Long customerId,
        Long customerCommunicationId,
        Long vatRateId,
        BigDecimal vatRatePercent,
        Boolean noInterestOnOverdueDebts,
        Long invoiceTemplateId
) {
}

