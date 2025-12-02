package bg.energo.phoenix.model.response.customer;

import bg.energo.phoenix.model.entity.customer.ConnectedGroup;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConnectedGroupResponse {
    private Long id;
    private String name;

    private String connectionTypeName;

    public ConnectedGroupResponse(ConnectedGroup group) {
        if(group!=null){
            this.id = group.getId();
            this.name = group.getName();
            this.connectionTypeName=group.getGccConnectionType().getName();
        }
    }
}
