package bg.energo.phoenix.virtualpos.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VirtualPosConfirmPaymentResponse {

    @JsonProperty("STATUS")
    @Schema(name = "STATUS")
    private String STATUS;

    @JsonProperty("DESCRIPTION")
    @Schema(name = "DESCRIPTION")
    private String DESCRIPTION;

    @JsonProperty("ADDITIONALINFO")
    @Schema(name = "ADDITIONALINFO")
    private String ADDITIONALINFO;
}


