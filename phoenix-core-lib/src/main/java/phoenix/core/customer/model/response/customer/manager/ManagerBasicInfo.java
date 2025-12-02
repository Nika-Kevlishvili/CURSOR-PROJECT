package phoenix.core.customer.model.response.customer.manager;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import phoenix.core.customer.model.entity.customer.Manager;

@Data
@AllArgsConstructor
public class ManagerBasicInfo {

    private Long id;
    private Long customerDetailId;
    private String name;

    public ManagerBasicInfo(Manager manager) {
        this.id = manager.getId();
        this.customerDetailId = manager.getCustomerDetailId();
        this.name = manager.getName()
                .concat(StringUtils.isEmpty(manager.getMiddleName()) ? "" : " " + manager.getMiddleName())
                .concat(" ")
                .concat(manager.getSurname())
                .concat(" ")
                .concat("(" + manager.getJobPosition() + ")");
    }
}
