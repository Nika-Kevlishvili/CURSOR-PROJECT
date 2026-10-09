package bg.energo.phoenix.virtualpos.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VirtualPosInvoiceResponse {

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
}


