package bg.energo.phoenix.systech.receipt.api;

import bg.energo.phoenix.payment.receipt.model.PaymentReceiptDocumentModel;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SystechReceiptResponse {

    @JsonProperty("ErrorCode")
    private int ErrorCode;

    @JsonProperty("ErrorMessage")
    private String ErrorMessage;

    @JsonProperty("Currency")
    private String Currency;

    @JsonProperty("Exchange")
    private BigDecimal Exchange;

    @JsonProperty("Principal")
    private BigDecimal Principal;

    @JsonProperty("Interest")
    private BigDecimal Interest;

    @JsonProperty("Another")
    private BigDecimal Another;

    @JsonProperty("Receipt")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private Object Receipt;

    public static SystechReceiptResponse success(
            String currency,
            BigDecimal exchange,
            BigDecimal principal,
            BigDecimal interest,
            BigDecimal another,
            Object receipt
    ) {
        return new SystechReceiptResponse(
                0,
                "",
                currency,
                exchange,
                principal,
                interest,
                another,
                receipt
        );
    }

    public static SystechReceiptResponse success(
            String currency,
            BigDecimal exchange,
            BigDecimal principal,
            BigDecimal interest,
            BigDecimal another,
            List<PaymentReceiptDocumentModel> receipts
    ) {
        Object receiptPayload = receipts == null || receipts.isEmpty()
                ? null
                : receipts.size() == 1 ? receipts.get(0) : receipts;
        return success(currency, exchange, principal, interest, another, receiptPayload);
    }

    public static SystechReceiptResponse error(int code, String message) {
        return new SystechReceiptResponse(code, message, null, null, null, null, null, null);
    }
}

