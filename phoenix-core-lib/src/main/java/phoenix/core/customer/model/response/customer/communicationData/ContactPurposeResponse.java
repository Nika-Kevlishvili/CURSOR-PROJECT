package phoenix.core.customer.model.response.customer.communicationData;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactPurposeResponse {

    private Long customerCommunicationsDataId;

    private String purposeName;

}
