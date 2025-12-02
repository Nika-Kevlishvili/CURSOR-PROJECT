package bg.energo.phoenix.model.response.customer;

import bg.energo.phoenix.model.entity.customer.ConnectedGroup;
import bg.energo.phoenix.model.enums.customer.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConnectedGroupDetailResponse {
    private Long id;
    private String name;
    private String additionalInfo;
    private Long typeOfConnectionId;
    private String typeOfConnectionName;
    private List<ConnectedGroupCustomerResponse> customerInfos;
    private Status status;

    public ConnectedGroupDetailResponse(ConnectedGroup group) {
        if (group != null) {
            this.id = group.getId();
            this.name = group.getName();
            this.additionalInfo = group.getAdditionalInfo();
            this.typeOfConnectionId = group.getGccConnectionType().getId();
            this.typeOfConnectionName = group.getGccConnectionType().getName();
            this.status=group.getStatus();
        }
    }
}
