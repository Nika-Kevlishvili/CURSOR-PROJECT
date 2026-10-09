package bg.energo.phoenix.cashterminal.model.response;

import bg.energo.phoenix.cashterminal.model.CashTerminalInvoiceResponse;
import bg.energo.phoenix.cashterminal.model.enums.CashTerminalStatusCode;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CashTerminalInitPaymentResponse {

    public CashTerminalInitPaymentResponse(CashTerminalStatusCode statusCode) {
        this.STATUS = statusCode.getCode();
    }

    @JsonProperty("STATUS")
    @Schema(name = "STATUS")
    private String STATUS;

    @JsonProperty("IDN")
    @Schema(name = "IDN")
    private String IDN;

    @JsonProperty("SHORTDESC")
    @Schema(name = "SHORTDESC")
    private String SHORTDESC;

    @JsonProperty("LONGDESC")
    @Schema(name = "LONGDESC")
    private String LONGDESC;

    @JsonProperty("AMOUNT")
    @Schema(name = "AMOUNT")
    private Long AMOUNT;

    @JsonProperty("VALIDTO")
    @Schema(name = "VALIDTO")
    private String VALIDTO;

    @JsonProperty("INVOICES")
    @Schema(name = "INVOICES")
    private List<CashTerminalInvoiceResponse> INVOICES;
}

