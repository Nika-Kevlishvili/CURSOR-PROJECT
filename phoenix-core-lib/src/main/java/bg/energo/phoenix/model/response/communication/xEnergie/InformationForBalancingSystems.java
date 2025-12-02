package bg.energo.phoenix.model.response.communication.xEnergie;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InformationForBalancingSystems {
    private Long id;
    private String productName;
    private String profileName;
    private String profileDescription;
}
