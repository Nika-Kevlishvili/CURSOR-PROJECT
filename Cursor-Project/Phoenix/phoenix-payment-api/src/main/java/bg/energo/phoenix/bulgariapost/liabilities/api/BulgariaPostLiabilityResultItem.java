package bg.energo.phoenix.bulgariapost.liabilities.api;

import bg.energo.phoenix.bulgariapost.liabilities.repository.BulgariaPostObligationsProjection;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulgariaPostLiabilityResultItem {
    @JsonProperty("CustomerNumber")
    private Long CustomerNumber;

    @JsonProperty("CustomerName")
    private String CustomerName;

    @JsonProperty("CustomerAddress")
    private String CustomerAddress;

    @JsonProperty("DocumentNumber")
    private String DocumentNumber;

    @JsonProperty("DocumentDate")
    private String DocumentDate;

    @JsonProperty("DocumentInfo")
    private String DocumentInfo;

    @JsonProperty("AllowedForPayment")
    private boolean AllowedForPayment;

    @JsonProperty("AllowedPartialPayment")
    private boolean AllowedPartialPayment;

    @JsonProperty("Currency")
    private String Currency;

    @JsonProperty("Principal")
    private BigDecimal Principal;

    @JsonProperty("Interest")
    private BigDecimal Interest;

    @JsonProperty("Another")
    private BigDecimal Another;


    public BulgariaPostLiabilityResultItem(BulgariaPostObligationsProjection r) {
        this(
                r.getCustomerNumber() == null ? null : Long.valueOf(r.getCustomerNumber()),
                nullToEmpty(r.getCustomerName()),
                nullToEmpty(r.getCustomerAddress()),
                nullToEmpty(r.getDocumentNumber()),
                nullToEmpty(r.getDocumentDate()),
                nullToEmpty(r.getDocumentInfo()),
                r.isAllowedForPayment(),
                r.isAllowedPartialPayment(),
                nullToEmpty(r.getCurrency()),
                r.getPrincipal(),
                r.getInterest(),
                r.getAnother()
        );
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}

