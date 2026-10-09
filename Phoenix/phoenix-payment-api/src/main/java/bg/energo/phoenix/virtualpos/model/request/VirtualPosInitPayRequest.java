package bg.energo.phoenix.virtualpos.model.request;

import bg.energo.phoenix.virtualpos.model.enums.VirtualPosRequestType;
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
public class VirtualPosInitPayRequest {

    //CustomerId
    @NotNull(message = "Check customer identifier must not be null;")
    @JsonProperty("IDN")
    @Schema(name = "IDN")
    private String IDN;

    // A unique identifier for each individual transaction.
    // The repetition is always the same. TID is formed by:
    // DATE Date and time, accurate to the second. The time can be from 00:00:00 to 23:59:59
    // STAN Service information of the Operator - not processed
    // AID Payment Source. They are divided into two types - 70002x and 7001xx are cash payments from Izipey.
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
    private VirtualPosRequestType TYPE;
}


