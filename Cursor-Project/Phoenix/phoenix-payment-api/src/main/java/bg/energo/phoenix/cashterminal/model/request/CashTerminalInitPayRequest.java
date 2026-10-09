package bg.energo.phoenix.cashterminal.model.request;

import bg.energo.phoenix.cashterminal.model.enums.CashTerminalRequestType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CashTerminalInitPayRequest {

    //CustomerId
    @NotNull(message = "Check customer identifier must not be null;")
    @JsonProperty("IDN")
    @Schema(name = "IDN")
    private String IDN;

    // A unique identifier for each individual transaction.
    @JsonProperty("TID")
    @Schema(name = "TID")
    private String TID;

    @NotNull(message = "Merchant id must not be null;")
    @JsonProperty("MERCHANTID")
    @Schema(name = "MERCHANTID")
    private String MERCHANTID;

    //Request type// Can be CHECK,BILLING
    @NotNull(message = "Type must not be null;")
    @JsonProperty("TYPE")
    @Schema(name = "TYPE")
    private CashTerminalRequestType TYPE;
}

