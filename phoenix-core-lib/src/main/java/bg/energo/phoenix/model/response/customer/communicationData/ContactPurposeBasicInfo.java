package bg.energo.phoenix.model.response.customer.communicationData;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContactPurposeBasicInfo {

    private Long id;
    private Long contactPurposeId;
    private String contactPurposeName;

}
