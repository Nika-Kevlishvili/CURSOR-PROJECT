package bg.energo.phoenix.model.response.customer.communicationData.detailed;

import bg.energo.phoenix.model.enums.customer.ContractPurposeType;
import bg.energo.phoenix.model.enums.customer.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactPurposeDetailedResponse {
    private Long contactPurposeId; // record id
    private Long id; // contact purpose id
    private String name; // contact purpose name
    private Long customerCommunicationsId;
    private ContractPurposeType contractPurposeType;
    private Status status;

    public ContactPurposeDetailedResponse(Long contactPurposeId, Long id, String name, Long customerCommunicationsId, Status status) {
        this.contactPurposeId = contactPurposeId;
        this.id = id;
        this.name = name;
        this.customerCommunicationsId = customerCommunicationsId;
        this.status = status;
    }
}
